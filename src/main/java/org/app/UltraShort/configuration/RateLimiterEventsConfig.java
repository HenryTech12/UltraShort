package org.app.UltraShort.configuration;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterEventsConfig {

    public RateLimiterEventsConfig(RateLimiterRegistry rateLimiterRegistry) {
        rateLimiterRegistry.getAllRateLimiters()
                .forEach(r -> r.getEventPublisher()
                        .onSuccess(event -> System.out.println("Call Allowed: "+event))
                        .onFailure(event -> System.out.println("Call Blocked: "+event))
                );
    }
}
