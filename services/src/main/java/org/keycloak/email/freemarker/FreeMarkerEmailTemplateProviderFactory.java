/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.email.freemarker;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerEmailTemplateProviderFactory implements EmailTemplateProviderFactory {

    static final String DELIVERY_THREADS = "delivery-threads";
    static final String DELIVERY_QUEUE_SIZE = "delivery-queue-size";
    private static final int DEFAULT_DELIVERY_THREADS = 4;
    private static final int DEFAULT_DELIVERY_QUEUE_SIZE = 500;
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);

    private EmailDeliveryQueue deliveryQueue;

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        return new FreeMarkerEmailTemplateProvider(session, deliveryQueue);
    }

    @Override
    public void init(Config.Scope config) {
        int threads = config.getInt(DELIVERY_THREADS, DEFAULT_DELIVERY_THREADS);
        if (threads < 1) {
            throw new IllegalArgumentException(DELIVERY_THREADS + " must be at least 1, but was " + threads);
        }
        int queueSize = config.getInt(DELIVERY_QUEUE_SIZE, DEFAULT_DELIVERY_QUEUE_SIZE);
        if (queueSize < 0) {
            throw new IllegalArgumentException(DELIVERY_QUEUE_SIZE + " must not be negative, but was " + queueSize);
        }
        deliveryQueue = new EmailDeliveryQueue(threads, queueSize);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        if (deliveryQueue != null) {
            deliveryQueue.close(CLOSE_TIMEOUT);
        }
    }

    @Override
    public String getId() {
        return "freemarker";
    }

    // closed before the realm and user providers, which the waiting emails need. Other providers may be closed already
    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(RealmProvider.class, UserProvider.class);
    }

    protected EmailDeliveryQueue getDeliveryQueue() {
        return deliveryQueue;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(DELIVERY_THREADS)
                .type("int")
                .helpText("Number of emails sent in the background at the same time. A realm can use at most half of them.")
                .defaultValue(DEFAULT_DELIVERY_THREADS)
                .add()
                .property()
                .name(DELIVERY_QUEUE_SIZE)
                .type("int")
                .helpText("Number of emails that can wait to be sent in the background, on top of the ones being sent. When it is full, further emails are dropped. A realm can have at most half of all the emails being sent or waiting.")
                .defaultValue(DEFAULT_DELIVERY_QUEUE_SIZE)
                .add()
                .build();
    }

}
