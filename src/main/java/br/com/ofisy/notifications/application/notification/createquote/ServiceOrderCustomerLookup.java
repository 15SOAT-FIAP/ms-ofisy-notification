package br.com.ofisy.notifications.application.notification.createquote;

import java.util.UUID;

public interface ServiceOrderCustomerLookup {
    UUID findCustomerId(UUID serviceOrderId);
}
