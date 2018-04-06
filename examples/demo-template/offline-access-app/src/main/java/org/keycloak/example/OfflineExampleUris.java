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

package org.keycloak.example;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineExampleUris {


    public static final String LOGIN_CLASSIC = "/offline-access-portal/app/login";


    public static final String LOGIN_WITH_OFFLINE_TOKEN = "/offline-access-portal/app/login?scope=offline_access";


    public static final String LOAD_CUSTOMERS = "/offline-access-portal/app/loadCustomers";


    public static final String ACCOUNT_MGMT = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH + "/applications")
            .queryParam("referrer", "offline-access-portal").build("demo").toString();


    public static final String LOGOUT = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/offline-access-portal").build("demo").toString();
}
