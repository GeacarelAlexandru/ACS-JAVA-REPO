package com.instakill.notification.application;

/**
 * Port for pushing a notification to an external service. Implementations must not throw for delivery problems
 * that the caller cannot act on; they log and return instead.
 */
public interface NotificationSender {

    void send(String title, String message);
}
