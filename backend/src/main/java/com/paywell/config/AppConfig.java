package com.paywell.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** All business timestamps are UTC instants; the clock is a bean so tests can fix it. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ZoneId userZone(PaywellProperties props) {
        return ZoneId.of(props.timezone());
    }
}
