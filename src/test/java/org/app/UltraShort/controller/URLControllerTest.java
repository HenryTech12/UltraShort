package org.app.UltraShort.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.UltraShort.configuration.AppConfiguration;
import org.app.UltraShort.exceptions.URLNotFoundException;
import org.app.UltraShort.model.URL;
import org.app.UltraShort.request.URLRequest;
import org.app.UltraShort.response.URLResponse;
import org.app.UltraShort.service.URLService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API contract tests for URLController. The URLService is mocked so these
 * run without needing a real MySQL/Redis connection. AppConfiguration is
 * excluded because it wires Redis-backed beans (cache manager, RedisTemplate)
 * that this slice has no RedisConnectionFactory to satisfy.
 */
@WebMvcTest(
        controllers = URLController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AppConfiguration.class)
)
@Import(URLControllerTest.CacheTestConfig.class)
class URLControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private URLService urlService;

    @TestConfiguration
    static class CacheTestConfig {
        // @EnableCaching (on the main application class) requires a
        // CacheManager bean to be present even though this controller
        // slice never actually invokes a cached method.
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    // ---------- POST /short ----------

    @Test
    void shortenUrl_validRequest_returns200WithShortUrlAndId() throws Exception {
        URLRequest request = new URLRequest("https://www.example.com/page");
        URLResponse response = new URLResponse("http://localhost/abc123", "abc123");
        when(urlService.createShortURL(any(URLRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/short")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/abc123"))
                .andExpect(jsonPath("$.urlID").value("abc123"));
    }

    @Test
    void shortenUrl_blankUrl_returns400() throws Exception {
        URLRequest request = new URLRequest("");

        mockMvc.perform(post("/short")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenUrl_missingUrlField_returns400() throws Exception {
        mockMvc.perform(post("/short")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenUrl_urlExceedsMaxLength_returns400() throws Exception {
        String tooLong = "https://example.com/" + "a".repeat(2048);
        URLRequest request = new URLRequest(tooLong);

        mockMvc.perform(post("/short")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenUrl_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/short")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenUrl_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post("/short")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("https://www.example.com"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ---------- GET /{urlId} ----------

    @Test
    void callUrl_found_redirectsWithLocationHeader() throws Exception {
        URL url = new URL();
        url.setUrlID("abc123");
        url.setUrl("https://www.example.com/target");
        when(urlService.fetchURL("abc123")).thenReturn(url);

        MvcResult mvcResult = mockMvc.perform(get("/abc123"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.example.com/target"));
    }

    @Test
    void callUrl_notFound_returns404WithErrorBody() throws Exception {
        when(urlService.fetchURL("missing")).thenThrow(new URLNotFoundException("URL not found"));

        MvcResult mvcResult = mockMvc.perform(get("/missing"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("URL not found"));
    }

    // ---------- GET /ping ----------

    @Test
    void ping_returns200WithSuccessMessage() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("Request successful...."));
    }
}
