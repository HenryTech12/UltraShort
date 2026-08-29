package org.app.UltraShort.integration;

import org.app.UltraShort.repository.URLRepository;
import org.app.UltraShort.request.URLRequest;
import org.app.UltraShort.response.URLResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests that boot the real application (embedded Tomcat, H2
 * standing in for MySQL, a local Redis instance) and exercise every API
 * over real HTTP, exactly as a client would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class URLApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private URLRepository urlRepository;

    @AfterEach
    void cleanUp() {
        urlRepository.deleteAll();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void ping_isReachableAndReturnsSuccessMessage() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/ping", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Request successful....");
    }

    @Test
    void shortenUrl_thenRedirect_fullRoundTrip() throws Exception {
        String longUrl = "https://www.example.com/articles/full-round-trip-test";

        ResponseEntity<URLResponse> shortenResponse = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest(longUrl), URLResponse.class);

        assertThat(shortenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        URLResponse body = shortenResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.urlID()).isNotBlank();
        assertThat(body.shortUrl()).endsWith("/" + body.urlID());

        assertThat(urlRepository.findByUrlID(body.urlID())).isPresent();

        // Plain HttpURLConnection with redirect-following disabled: the
        // target URL is an external, non-routable domain in this test
        // environment, so we must inspect the 302 without letting any HTTP
        // client actually follow it.
        HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl() + "/" + body.urlID())
                .toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");

        assertThat(connection.getResponseCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(connection.getHeaderField("Location")).isEqualTo(longUrl);
        connection.disconnect();
    }

    @Test
    void callUrl_unknownId_returns404WithStructuredErrorBody() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl() + "/does-not-exist", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
    }

    @Test
    void shortenUrl_blankUrl_returns400() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest(""), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shortenUrl_twoDifferentUrls_getDistinctShortIds() {
        ResponseEntity<URLResponse> first = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest("https://www.example.com/one"), URLResponse.class);
        ResponseEntity<URLResponse> second = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest("https://www.example.com/two"), URLResponse.class);

        assertThat(first.getBody()).isNotNull();
        assertThat(second.getBody()).isNotNull();
        assertThat(first.getBody().urlID()).isNotEqualTo(second.getBody().urlID());
    }

    @Test
    void shortenUrl_sameUrlSubmittedTwice_documentsCurrentBehavior() {
        // NOTE: this documents a real product bug found while writing this
        // suite (see PR description / feedback for the full write-up).
        // The `url` column is unique, and createShortURL() only re-derives
        // a new short id on collision, not a fresh save target. Resubmitting
        // the exact same long URL fails to save every single retry attempt
        // with the same DataIntegrityViolationException, so instead of a
        // clean 400 "duplicate URL" response, @Retry burns through all 4
        // attempts, retryOnException's registry (which is misconfigured -
        // see ResilienceConfigTest / feedback) treats it as retryable, and
        // the request ultimately fails with a misleading 500 "retry
        // mechanism failed" instead of ever surfacing the real cause.
        String longUrl = "https://www.example.com/resubmitted-url";

        ResponseEntity<URLResponse> first = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest(longUrl), URLResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> second = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest(longUrl), Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(second.getBody()).containsEntry("error", "Retry failed!!!");
    }
}
