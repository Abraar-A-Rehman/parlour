package com.parlour.api.service;

/**
 * Abstraction over an SMS gateway. For production, write another implementation
 * (SSL Wireless, BulkSMSBD, Twilio ...) and mark it @Primary / replace ConsoleSmsSender.
 */
public interface SmsSender {
    void send(String phone, String message);
}
