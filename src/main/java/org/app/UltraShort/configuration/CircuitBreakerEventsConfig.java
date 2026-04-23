package org.app.UltraShort.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerEventsConfig {

    public CircuitBreakerEventsConfig(CircuitBreakerRegistry circuitBreakerRegistry) {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach((d) -> d.getEventPublisher()
                .onStateTransition(event -> System.out.println("State Transition: "+event))
                .onCallNotPermitted(event -> System.out.println("Call Not Permitted: "+event))
                .onError(event -> System.out.println("Call Failed: "+event))
                .onSuccess(event -> System.out.println("Call Successful: "+event))
        );
    }
}
