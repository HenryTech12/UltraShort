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
import org.springframework.test.annotation.DirtiesContext;

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
    void shortenUrl_sameUrlSubmittedTwice_failsFastWithoutBurningRetries() {
        // The `url` column is unique, and createShortURL() only re-derives
        // a new short id on collision, not a fresh save target, so
        // resubmitting the exact same long URL can never succeed. Retry is
        // now configured to not retry DataIntegrityViolationException, so
        // this fails on the first attempt with a clean 400 instead of
        // burning all 4 retry attempts and returning a misleading 500.
        String longUrl = "https://www.example.com/resubmitted-url";

        ResponseEntity<URLResponse> first = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest(longUrl), URLResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> second = restTemplate.postForEntity(
                baseUrl() + "/short", new URLRequest(longUrl), Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    // This test deliberately saturates the shared rate limiter / circuit
    // breaker singletons; force a fresh Spring context afterwards so it
    // doesn't leak throttled state into whichever test method runs next.
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void shortenUrl_concurrentLegitimateRequests_areThrottledNotFailed() throws InterruptedException {
        // Regression test for the retry-storm bug: previously, retrying
        // straight back into an already-exhausted rate limiter meant a
        // burst of concurrent, perfectly valid requests mostly failed with
        // misleading 500s. Now that Retry/CircuitBreaker only convert the
        // exception they actually own and let everything else through
        // untouched, 10 concurrent requests against a rate limiter tuned to
        // 3 permits/2s legitimately throttles some of them - but as a clean,
        // correctly-classified 429, never a 500.
        int threadCount = 10;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.List<Integer> statuses = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int idx = i;
            pool.submit(() -> {
                try {
                    ResponseEntity<URLResponse> response = restTemplate.postForEntity(baseUrl() + "/short",
                            new URLRequest("https://www.example.com/concurrent-" + idx + "-" + System.nanoTime()),
                            URLResponse.class);
                    statuses.add(response.getStatusCode().value());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        assertThat(statuses).hasSize(threadCount);
        // Never a 500: every request is either served or cleanly throttled.
        assertThat(statuses).allMatch(status -> status == HttpStatus.OK.value()
                || status == HttpStatus.TOO_MANY_REQUESTS.value());
        // The majority of legitimate concurrent traffic still gets through -
        // 3 permits/2s with a 5s timeout can't fully drain 10 truly
        // concurrent callers, so some throttling here is expected, not a bug.
        long succeeded = statuses.stream().filter(status -> status == HttpStatus.OK.value()).count();
        assertThat(succeeded).isGreaterThanOrEqualTo(threadCount / 2);
    }
}
