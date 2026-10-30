package com.acme.provider.legacy.jpa.entity;

import jakarta.persistence.ValidationMode;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.AvailableSettings;
import org.jboss.logging.Logger;

public class ValidationModeObserver implements SessionFactoryObserver {

    @Override
    public void sessionFactoryCreated(SessionFactory factory) {
        Object mode = factory.getProperties().get(AvailableSettings.JAKARTA_VALIDATION_MODE);
        if (!ValidationMode.NONE.name().equals(String.valueOf(mode))) {
            throw new IllegalStateException("Expected validation mode NONE for the custom persistence unit, got " + mode);
        }
        Logger.getLogger(ValidationModeObserver.class).infof("Custom persistence unit validation mode: %s", mode);
    }
}
