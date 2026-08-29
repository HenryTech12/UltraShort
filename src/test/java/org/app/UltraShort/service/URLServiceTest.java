package org.app.UltraShort.service;

import jakarta.servlet.http.HttpServletRequest;
import org.app.UltraShort.exceptions.URLNotFoundException;
import org.app.UltraShort.model.URL;
import org.app.UltraShort.repository.URLRepository;
import org.app.UltraShort.request.URLRequest;
import org.app.UltraShort.response.URLResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class URLServiceTest {

    @Mock
    private URLRepository urlRepository;

    @Mock
    private RedisTemplate<String, URL> redisTemplate;

    @Mock
    private ValueOperations<String, URL> valueOperations;

    @InjectMocks
    private URLService urlService;

    private HttpServletRequest requestMock;

    @BeforeEach
    void setUp() {
        requestMock = mock(HttpServletRequest.class);
    }

    private void stubRequestUrl() {
        when(requestMock.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/short"));
        when(requestMock.getRequestURI()).thenReturn("/short");
    }

    @Test
    void createShortURL_happyPath_savesAndCachesUrl() {
        stubRequestUrl();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        URLRequest request = new URLRequest("https://www.example.com");
        when(urlRepository.findByUrlID(anyString())).thenReturn(Optional.empty());

        URLResponse response = urlService.createShortURL(request, requestMock);

        assertThat(response.urlID()).isNotBlank();
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/" + response.urlID());

        ArgumentCaptor<URL> savedUrl = ArgumentCaptor.forClass(URL.class);
        verify(urlRepository).save(savedUrl.capture());
        assertThat(savedUrl.getValue().getUrl()).isEqualTo("https://www.example.com");
        assertThat(savedUrl.getValue().getUrlID()).isEqualTo(response.urlID());

        verify(valueOperations).set(eq(response.urlID()), any(URL.class), any(Duration.class));
    }

    @Test
    void createShortURL_hashCollision_regeneratesId() {
        stubRequestUrl();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        URLRequest request = new URLRequest("https://www.example.com/collision");
        String originalHash = HashService.generateHash(request.url());

        when(urlRepository.findByUrlID(originalHash)).thenReturn(Optional.of(new URL()));

        URLResponse response = urlService.createShortURL(request, requestMock);

        assertThat(response.urlID()).isNotEqualTo(originalHash);
        verify(urlRepository).save(any(URL.class));
    }

    @Test
    void createShortURL_dataIntegrityViolation_regeneratesIdAndRetriesSave() {
        stubRequestUrl();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        URLRequest request = new URLRequest("https://www.example.com/dup");
        when(urlRepository.findByUrlID(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(URL.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        URLResponse response = urlService.createShortURL(request, requestMock);

        assertThat(response.urlID()).isNotBlank();
        assertThat(response.shortUrl()).contains(response.urlID());
        verify(urlRepository, times(2)).save(any(URL.class));
    }

    @Test
    void generateApplicationURL_stripsRequestUriFromFullUrl() {
        stubRequestUrl();
        String appUrl = urlService.generateApplicationURL(requestMock);

        assertThat(appUrl).isEqualTo("http://localhost:8080");
    }

    @Test
    void fetchURL_cacheHit_returnsFromRedisWithoutHittingRepository() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        URL cached = new URL();
        cached.setUrlID("abc123");
        cached.setUrl("https://cached.example.com");
        when(valueOperations.get("abc123")).thenReturn(cached);

        URL result = urlService.fetchURL("abc123");

        assertThat(result).isEqualTo(cached);
        verifyNoInteractions(urlRepository);
    }

    @Test
    void fetchURL_cacheMiss_fallsBackToRepositoryAndRepopulatesCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        URL fromDb = new URL();
        fromDb.setUrlID("xyz789");
        fromDb.setUrl("https://db.example.com");
        when(valueOperations.get("xyz789")).thenReturn(null);
        when(urlRepository.findByUrlID("xyz789")).thenReturn(Optional.of(fromDb));
        when(valueOperations.setIfAbsent(eq("xyz789"), eq(fromDb), any(Duration.class))).thenReturn(true);

        URL result = urlService.fetchURL("xyz789");

        assertThat(result).isEqualTo(fromDb);
        verify(valueOperations).setIfAbsent(eq("xyz789"), eq(fromDb), any(Duration.class));
    }

    @Test
    void fetchURL_notFoundAnywhere_throwsURLNotFoundException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("missing")).thenReturn(null);
        when(urlRepository.findByUrlID("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.fetchURL("missing"))
                .isInstanceOf(URLNotFoundException.class);

        verify(valueOperations, never()).setIfAbsent(anyString(), any(URL.class), any(Duration.class));
    }
}
