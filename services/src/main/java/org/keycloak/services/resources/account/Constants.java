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
package org.keycloak.services.resources.account;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Constants {

    public static final EventType[] EXPOSED_LOG_EVENTS = {
            EventType.LOGIN, EventType.LOGOUT, EventType.REGISTER, EventType.REMOVE_FEDERATED_IDENTITY, EventType.REMOVE_TOTP, EventType.SEND_RESET_PASSWORD,
            EventType.SEND_VERIFY_EMAIL, EventType.FEDERATED_IDENTITY_LINK, EventType.UPDATE_EMAIL, EventType.UPDATE_PASSWORD, EventType.UPDATE_PROFILE, EventType.UPDATE_TOTP, EventType.VERIFY_EMAIL
    };

    public static final Set<String> EXPOSED_LOG_DETAILS = new HashSet<>();

    static {
        EXPOSED_LOG_DETAILS.add(Details.UPDATED_EMAIL);
        EXPOSED_LOG_DETAILS.add(Details.EMAIL);
        EXPOSED_LOG_DETAILS.add(Details.PREVIOUS_EMAIL);
        EXPOSED_LOG_DETAILS.add(Details.FIRST_NAME);
        EXPOSED_LOG_DETAILS.add(Details.LAST_NAME);
        EXPOSED_LOG_DETAILS.add(Details.UPDATED_FIRST_NAME);
        EXPOSED_LOG_DETAILS.add(Details.PREVIOUS_FIRST_NAME);
        EXPOSED_LOG_DETAILS.add(Details.UPDATED_LAST_NAME);
        EXPOSED_LOG_DETAILS.add(Details.PREVIOUS_LAST_NAME);
        EXPOSED_LOG_DETAILS.add(Details.USERNAME);
        EXPOSED_LOG_DETAILS.add(Details.REMEMBER_ME);
        EXPOSED_LOG_DETAILS.add(Details.REGISTER_METHOD);
        EXPOSED_LOG_DETAILS.add(Details.AUTH_METHOD);
    }

}
