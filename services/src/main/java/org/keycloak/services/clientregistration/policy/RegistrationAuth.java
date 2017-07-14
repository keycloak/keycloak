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

package org.keycloak.services.clientregistration.policy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum RegistrationAuth {

    /**
     * Case when client is registered without token (either initialAccessToken or BearerToken).
     *
     * Note this will be the case also for update + view + remove with registrationToken, which was created during anonymous registration
     */
    ANONYMOUS,

    /**
     * Case when client is registered with token (either initialAccessToken or BearerToken).
     *
     * Note this will be the case also update + view + remove with registrationToken, which was created during authenticated registration or via admin console
     */
    AUTHENTICATED;

    public static RegistrationAuth fromString(String regAuth) {
        return Enum.valueOf(RegistrationAuth.class, regAuth.toUpperCase());
    }

}
