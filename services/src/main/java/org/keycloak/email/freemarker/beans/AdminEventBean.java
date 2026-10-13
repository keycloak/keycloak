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

package org.keycloak.email.freemarker.beans;

import java.util.Date;

import org.keycloak.events.admin.AdminEvent;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class AdminEventBean {

    private AdminEvent adminEvent;

    public AdminEventBean(AdminEvent adminEvent) {
        this.adminEvent = adminEvent;
    }

    public Date getDate() {
        return new Date(adminEvent.getTime());
    }

    public String getOperationType() {
        return adminEvent.getOperationType().toString().toLowerCase();
    }

    public String getClient() {
        return adminEvent.getAuthDetails().getClientId();
    }

    /**
     * Note: will not be an address when a proxy does not provide a valid one
     *
     * @return the ip address
     */
    public String getIpAddress() {
        return adminEvent.getAuthDetails().getIpAddress();
    }

    public String getResourcePath() {
        return adminEvent.getResourcePath();
    }
}
