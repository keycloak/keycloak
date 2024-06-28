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
package org.keycloak.broker.provider;

/**
 * @author pedroigor
 */
public class IdentityBrokerException extends RuntimeException {
    private String messageCode;
    public IdentityBrokerException(String message) {
        super(message);
    }

    public IdentityBrokerException(String message, Throwable t) {
        super(message, t);
    }

    public IdentityBrokerException withMessageCode(String messageCode) {
        this.messageCode = messageCode;
        return this;
    }

    public String getMessageCode() {
        return messageCode;
    }
}
