/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.ipatuura_user_spi;

import java.io.IOException;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;

public class IpatuuraUserModelDelegate extends UserModelDelegate {

    private static final Logger logger = Logger.getLogger(IpatuuraUserModelDelegate.class);

    private ComponentModel model;

    private final Ipatuura ipatuura;

    public IpatuuraUserModelDelegate(Ipatuura ipatuura, UserModel delegate, ComponentModel model) {
        super(delegate);
        this.model = model;
        this.ipatuura = ipatuura;
    }

    @Override
    public void setAttribute(String attr, List<String> values) {
        SimpleHttpResponse resp = this.ipatuura.updateUser(ipatuura, this.getUsername(), attr, values);
        try {
            if (resp.getStatus() != HttpStatus.SC_OK && resp.getStatus() != HttpStatus.SC_NO_CONTENT) {
                logger.warn("Unexpected PUT status code returned");
                resp.close();
                return;
            }
            resp.close();
        } catch (IOException e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }
        super.setAttribute(attr, values);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        super.setSingleAttribute(name, value);
    }

    @Override
    public void setUsername(String username) {
        super.setUsername(username);
    }

    @Override
    public void setLastName(String lastName) {
        super.setLastName(lastName);
    }

    @Override
    public void setFirstName(String first) {
        super.setFirstName(first);
    }

    @Override
    public void setEmail(String email) {
        super.setFirstName(email);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }
}
