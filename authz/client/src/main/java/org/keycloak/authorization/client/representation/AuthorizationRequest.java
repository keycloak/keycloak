/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.client.representation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationRequest {

    private String ticket;
    private String rpt;

    public AuthorizationRequest(String ticket, String rpt) {
        this.ticket = ticket;
        this.rpt = rpt;
    }

    public AuthorizationRequest(String ticket) {
        this(ticket, null);
    }

    public AuthorizationRequest() {
        this(null, null);
    }

    public String getTicket() {
        return this.ticket;
    }

    public String getRpt() {
        return this.rpt;
    }
}
