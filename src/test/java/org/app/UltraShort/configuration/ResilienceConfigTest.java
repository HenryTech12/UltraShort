package org.app.UltraShort.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in the resilience tuning values so a future change to
 * ResilienceConfig is a deliberate, visible decision rather than an
 * accidental regression.
 */
class ResilienceConfigTest {

    private final ResilienceConfig resilienceConfig = new ResilienceConfig();

    @Test
    void circuitBreakerConfig_matchesExpectedTuning() {
        CircuitBreakerRegistry registry = resilienceConfig.circuitBreakerConfig();
        CircuitBreakerConfig config = registry.getDefaultConfig();

        assertThat(config.getFailureRateThreshold()).isEqualTo(0.5f);
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(0.3f);
        assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(1));
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(10);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(5);
        assertThat(config.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
    }

    @Test
    void retryConfig_matchesExpectedTuning() {
        RetryRegistry registry = resilienceConfig.retryConfig();
        RetryConfig config = registry.getDefaultConfig();

        assertThat(config.getMaxAttempts()).isEqualTo(4);
    }

    @Test
    void rateLimiterConfig_matchesExpectedTuning() {
        RateLimiterRegistry registry = resilienceConfig.rateLimiterConfig();
        RateLimiterConfig config = registry.getDefaultConfig();

        assertThat(config.getLimitForPeriod()).isEqualTo(3);
        assertThat(config.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
    }
}
