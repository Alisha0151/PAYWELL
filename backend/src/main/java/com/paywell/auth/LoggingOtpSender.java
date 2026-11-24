package com.paywell.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default sender. It does NOT deliver anything and says so. It never logs the code, so a log reader cannot take over accounts.
 */
public class LoggingOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public boolean send(String destination, String purpose, String code) {
        log.warn("OTP generated for purpose={} but NO delivery provider is configured; nothing was sent.", purpose);
        return false;
    }
}
