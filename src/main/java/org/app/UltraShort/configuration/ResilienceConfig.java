package org.app.UltraShort.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.app.UltraShort.exceptions.ServerFailedException;
import org.app.UltraShort.exceptions.ServerManyRequestException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerConfig() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(0.5f)
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(3))
                .permittedNumberOfCallsInHalfOpenState(5)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slowCallRateThreshold(0.3f)
                .slowCallDurationThreshold(Duration.ofSeconds(1))
                // A client submitting a URL that's already been shortened is a
                // normal, expected outcome, not a sign the backend is unhealthy.
                // Without this, that 4xx-shaped failure counts toward the
                // circuit's failure rate and can trip it open for everyone.
                .ignoreExceptions(DataIntegrityViolationException.class, OptimisticLockingFailureException.class)
                .build();
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public RetryRegistry retryConfig() {
        RetryConfig retryConfig = RetryConfig.custom()
                .failAfterMaxAttempts(true)
                .maxAttempts(4)
                .waitDuration(Duration.ofSeconds(2))
                // Don't retry failures that another resilience layer (or the DB)
                // already made a final decision on - retrying them can't succeed
                // and just re-consumes rate limiter / circuit breaker capacity.
                .ignoreExceptions(
                        DataIntegrityViolationException.class,
                        OptimisticLockingFailureException.class,
                        ServerManyRequestException.class,
                        ServerFailedException.class
                )
                .build();
        return RetryRegistry.of(retryConfig);
    }

    @Bean
    public RateLimiterRegistry rateLimiterConfig() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofSeconds(2))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        return RateLimiterRegistry.of(rateLimiterConfig);
    }
}
