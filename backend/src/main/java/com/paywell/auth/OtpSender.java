package com.paywell.auth;

/** Port for delivering one-time codes (SMS/email). No real provider is integrated in this build. */
public interface OtpSender {
    /** @return true only if the code was actually handed to a delivery provider. */
    boolean send(String destination, String purpose, String code);

    /** Whether a real delivery channel is configured; drives honest UI/API messaging. */
    default boolean deliveryConfigured() { return false; }
}
