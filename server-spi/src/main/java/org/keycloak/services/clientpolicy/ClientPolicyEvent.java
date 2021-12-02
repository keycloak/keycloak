/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy;

/**
 * Events on which client policies mechanism detects and do its operation
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public enum ClientPolicyEvent {

    REGISTER,
    REGISTERED,
    UPDATE,
    UPDATED,
    VIEW,
    UNREGISTER,
    AUTHORIZATION_REQUEST,
    TOKEN_REQUEST,
    SERVICE_ACCOUNT_TOKEN_REQUEST,
    TOKEN_REFRESH,
    TOKEN_REVOKE,
    TOKEN_INTROSPECT,
    USERINFO_REQUEST,
    LOGOUT_REQUEST,
    BACKCHANNEL_AUTHENTICATION_REQUEST,
    BACKCHANNEL_TOKEN_REQUEST,
    PUSHED_AUTHORIZATION_REQUEST,
    DEVICE_AUTHORIZATION_REQUEST,
    DEVICE_TOKEN_REQUEST,
    RESOURCE_OWNER_PASSWORD_CREDENTIALS_REQUEST

}
