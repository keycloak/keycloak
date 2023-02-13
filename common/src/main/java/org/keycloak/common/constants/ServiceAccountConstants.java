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

package org.keycloak.common.constants;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ServiceAccountConstants {

    String CLIENT_AUTH = "client_auth";

    String SERVICE_ACCOUNT_USER_PREFIX = "service-account-";

    String CLIENT_ID_PROTOCOL_MAPPER = "Client ID";
    String CLIENT_HOST_PROTOCOL_MAPPER = "Client Host";
    String CLIENT_ADDRESS_PROTOCOL_MAPPER = "Client IP Address";
    String CLIENT_ID = "clientId";
    String CLIENT_HOST = "clientHost";
    String CLIENT_ADDRESS = "clientAddress";

}
