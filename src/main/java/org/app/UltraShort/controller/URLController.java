package org.app.UltraShort.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.app.UltraShort.model.URL;
import org.app.UltraShort.request.URLRequest;
import org.app.UltraShort.response.URLResponse;
import org.app.UltraShort.service.URLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Random;

@RestController
@Slf4j
public class URLController {

    @Autowired
    private URLService urlService;

    @PostMapping("/short")
    @Retry(name = "url_retry", fallbackMethod = "retryingBackendStart")
    @CircuitBreaker(name = "url_backend", fallbackMethod = "backendServerDown")
    @RateLimiter(name = "url-limiter", fallbackMethod = "limitRequest")
    public ResponseEntity<URLResponse> shortenURL(@RequestBody URLRequest urlRequest, HttpServletRequest request) {
        return ResponseEntity.ok().body(urlService.generateShortURL(urlRequest,request));
    }

    @GetMapping("/{urlId}")
    @Retry(name = "url_retry", fallbackMethod = "retryingBackendStart")
    @CircuitBreaker(name = "url_backend", fallbackMethod = "backendServerDown")
    @RateLimiter(name = "url-limiter", fallbackMethod = "limitRequest")
    @TimeLimiter(name = "url_time_limiter", fallbackMethod = "timeLimit")
    public ResponseEntity<Void> callURL(@PathVariable String urlId) throws Exception {
        URL url = urlService.fetchURL(urlId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.getUrl())).build();
    }

    public ResponseEntity<String> backendServerDown(Throwable t) {
        return ResponseEntity.status(500).body("Backend Server is failing, Circuit Breaker in action.");
    }


    public ResponseEntity<String> retryingBackendStart(Throwable t) {
        return ResponseEntity.status(500).body("Backend Server is starting back again, Retry in action.");
    }


    public ResponseEntity<String> limitRequest(Throwable t) {
        log.error("Too many requests....");
        return ResponseEntity.status(429).body("          Too many requests....");
    }

    public ResponseEntity<String> timeLimit(Throwable t) {
        log.error("requests is taking too long....");
        return ResponseEntity.status(500).body("requests is taking too long....");
    }
}
