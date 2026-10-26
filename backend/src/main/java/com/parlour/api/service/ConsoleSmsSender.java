package com.parlour.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Local-development "SMS gateway": prints the SMS in the backend console instead of sending it. */
@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.info("[SMS to {}] {}", phone, message);
    }
}
