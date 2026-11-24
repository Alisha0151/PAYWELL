package com.paywell.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Local development only (profiles dev/local + paywell.otp.provider=dev-echo): logs the code. Still reports "not delivered". */
public class DevEchoOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(DevEchoOtpSender.class);

    @Override
    public boolean send(String destination, String purpose, String code) {
        log.warn("DEV ONLY - OTP for purpose={} is {} (not delivered to any user)", purpose, code);
        return false;
    }
}
