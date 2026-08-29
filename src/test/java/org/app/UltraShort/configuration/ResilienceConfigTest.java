package org.app.UltraShort.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.app.UltraShort.exceptions.ServerFailedException;
import org.app.UltraShort.exceptions.ServerManyRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.util.function.Predicate;

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
    void circuitBreakerConfig_doesNotCountDuplicateUrlAsABackendFailure() {
        // A client resubmitting an already-shortened URL is normal usage,
        // not a backend health signal. Without this, enough of those in a
        // sliding window can trip the breaker open for every other user too.
        CircuitBreakerRegistry registry = resilienceConfig.circuitBreakerConfig();
        Predicate<Throwable> ignorePredicate = registry.getDefaultConfig().getIgnoreExceptionPredicate();

        assertThat(ignorePredicate.test(new DataIntegrityViolationException("duplicate"))).isTrue();
        assertThat(ignorePredicate.test(new RuntimeException("genuine backend failure"))).isFalse();
    }

    @Test
    void retryConfig_matchesExpectedTuning() {
        RetryRegistry registry = resilienceConfig.retryConfig();
        RetryConfig config = registry.getDefaultConfig();

        assertThat(config.getMaxAttempts()).isEqualTo(4);
    }

    @Test
    void retryConfig_doesNotRetryFailuresAlreadyFinalizedByAnotherLayer() {
        // Regression test for the retry-storm bug: the original predicate
        // unconditionally threw instead of returning a boolean, which meant
        // every exception - including rejections the rate limiter and
        // circuit breaker had already decided on, and duplicate-key DB
        // errors that can never succeed - was retried up to 4 times.
        RetryRegistry registry = resilienceConfig.retryConfig();
        Predicate<Throwable> retryPredicate = registry.getDefaultConfig().getExceptionPredicate();

        assertThat(retryPredicate.test(new DataIntegrityViolationException("duplicate"))).isFalse();
        assertThat(retryPredicate.test(new ServerManyRequestException())).isFalse();
        assertThat(retryPredicate.test(new ServerFailedException())).isFalse();
    }

    @Test
    void retryConfig_stillRetriesGenuinelyTransientFailures() {
        RetryRegistry registry = resilienceConfig.retryConfig();
        Predicate<Throwable> retryPredicate = registry.getDefaultConfig().getExceptionPredicate();

        assertThat(retryPredicate.test(new RuntimeException("transient blip"))).isTrue();
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
