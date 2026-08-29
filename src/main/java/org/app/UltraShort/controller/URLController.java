package org.app.UltraShort.controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.app.UltraShort.exceptions.RetryException;
import org.app.UltraShort.exceptions.ServerFailedException;
import org.app.UltraShort.exceptions.ServerManyRequestException;
import org.app.UltraShort.model.URL;
import org.app.UltraShort.request.URLRequest;
import org.app.UltraShort.response.URLResponse;
import org.app.UltraShort.service.URLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
    public ResponseEntity<URLResponse> shortenURL(@RequestBody @Valid URLRequest urlRequest, HttpServletRequest request) {
        return ResponseEntity.ok().body(urlService.createShortURL(urlRequest,request));
    }

    @GetMapping("/{urlId}")
    public CompletableFuture<ResponseEntity<Void>> callURL(@PathVariable String urlId) throws Exception {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(urlService.fetchURL(urlId).getUrl())).build());
    }

    // Resilience4j invokes a fallbackMethod for *any* exception the decorated
    // call ends with, not just the one that annotation's own concern (circuit
    // open / rate limited / retries exhausted). Each fallback below only
    // converts the exception it actually owns, and lets everything else
    // (validation errors, duplicate-URL conflicts, another layer's own
    // already-final exception) pass through untouched to CustomExceptionHandler
    // instead of being mislabelled and stacking three misleading conversions
    // on top of one real failure.
    public ResponseEntity<URLResponse> backendServerDown(Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.error("Backend failure : {}", t.getMessage());
            throw new ServerFailedException();
        }
        return sneakyRethrow(t);
    }

    public ResponseEntity<URLResponse> retryingBackendStart(Throwable t) {
        if (t instanceof DataIntegrityViolationException
                || t instanceof OptimisticLockingFailureException
                || t instanceof ServerManyRequestException
                || t instanceof ServerFailedException) {
            return sneakyRethrow(t);
        }
        log.error("retry exhausted: {}", t.getMessage());
        throw new RetryException();
    }

    public ResponseEntity<URLResponse> limitRequest(Throwable t) {
        if (t instanceof RequestNotPermitted) {
            log.error("Rate limit hit for IP: {}","127.0.0.1");
            throw new ServerManyRequestException();
        }
        return sneakyRethrow(t);
    }

    private <T> T sneakyRethrow(Throwable t) {
        if (t instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(t);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok().body("Request successful....");
    }
}
