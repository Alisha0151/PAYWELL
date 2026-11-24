package com.paywell.config;

import com.paywell.auth.DevEchoOtpSender;
import com.paywell.auth.LoggingOtpSender;
import com.paywell.auth.OtpSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class OtpConfig {

    /**
     * No SMS/email provider is integrated. By default codes are generated but NOT delivered (and not logged).
     * "dev-echo" is honoured only under the dev/local Spring profiles and logs the code for local testing.
     */
    @Bean
    public OtpSender otpSender(PaywellProperties props, Environment env) {
        if ("dev-echo".equalsIgnoreCase(props.otp().provider()) && env.acceptsProfiles(Profiles.of("dev", "local"))) {
            return new DevEchoOtpSender();
        }
        return new LoggingOtpSender();
    }
}
