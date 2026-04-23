package org.app.UltraShort.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.app.UltraShort.exceptions.RetryException;
import org.app.UltraShort.exceptions.ServerFailedException;
import org.app.UltraShort.exceptions.ServerManyRequestException;
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
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
public class URLController {

    @Autowired
    private URLService urlService;

    @PostMapping("/short")
    @Retry(name = "shortenUrlRetry", fallbackMethod = "retryingBackendStart")
    @CircuitBreaker(name = "shortenUrlCircuitBreaker",fallbackMethod = "backendServerDown")
    @RateLimiter(name = "shortenUrlRateLimiter",fallbackMethod = "limitRequest")
    public ResponseEntity<URLResponse> shortenURL(@RequestBody URLRequest urlRequest, HttpServletRequest request) {
        return ResponseEntity.ok().body(urlService.createShortURL(urlRequest,request));
    }

    @GetMapping("/{urlId}")
    public CompletableFuture<ResponseEntity<Void>> callURL(@PathVariable String urlId) throws Exception {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(urlService.fetchURL(urlId).getUrl())).build());
    }

    public ResponseEntity<URLResponse> backendServerDown(Throwable t) {
        log.error("Backend failure : {}", t.getMessage());
        throw new ServerFailedException();
    }


    public ResponseEntity<URLResponse> retryingBackendStart(Throwable t) {
        log.error("retry exhausted: {}", t.getMessage());
        throw new RetryException();
    }


    public ResponseEntity<URLResponse> limitRequest(Throwable t) {
        log.error("Rate limit hit for IP: {}","127.0.0.1");
        throw new ServerManyRequestException();
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok().body("Request successful....");
    }
}
