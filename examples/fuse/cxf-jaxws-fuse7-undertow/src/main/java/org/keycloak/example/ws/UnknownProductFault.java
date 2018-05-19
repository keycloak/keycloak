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

package org.keycloak.example.ws;

import javax.xml.ws.WebFault;

@WebFault(name = "UnknownProductFault")
public class UnknownProductFault extends Exception {

    private org.keycloak.example.ws.types.UnknownProductFault unknownProductFault;

    public UnknownProductFault() {
        super();
    }

    public UnknownProductFault(String message) {
        super(message);
    }

    public UnknownProductFault(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownProductFault(String message, org.keycloak.example.ws.types.UnknownProductFault unknownProductFault) {
        super(message);
        this.unknownProductFault = unknownProductFault;
    }

    public UnknownProductFault(String message, org.keycloak.example.ws.types.UnknownProductFault unknownProductFault, Throwable cause) {
        super(message, cause);
        this.unknownProductFault = unknownProductFault;
    }

    public org.keycloak.example.ws.types.UnknownProductFault getFaultInfo() {
        return this.unknownProductFault;
    }
}
