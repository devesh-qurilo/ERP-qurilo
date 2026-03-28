package com.erp.client_service.event.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientCreatedEvent extends ApplicationEvent {
    private final Long id;
    private final String clientId;
    private final String email;
    private final String addedBy;

    public ClientCreatedEvent(Object source, Long id, String clientId, String email, String addedBy) {
        super(source);
        this.id = id;
        this.clientId = clientId;
        this.email = email;
        this.addedBy = addedBy;
    }
}
