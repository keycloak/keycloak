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

package org.keycloak.events;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum EventType {

    LOGIN(true),
    LOGIN_ERROR(true),
    REGISTER(true),
    REGISTER_ERROR(true),
    LOGOUT(true),
    LOGOUT_ERROR(true),

    CODE_TO_TOKEN(true),
    CODE_TO_TOKEN_ERROR(true),

    CLIENT_LOGIN(true),
    CLIENT_LOGIN_ERROR(true),

    REFRESH_TOKEN(false),
    REFRESH_TOKEN_ERROR(false),

    /**
     * @deprecated see KEYCLOAK-2266
     */
    @Deprecated
    VALIDATE_ACCESS_TOKEN(false),
    @Deprecated
    VALIDATE_ACCESS_TOKEN_ERROR(false),
    INTROSPECT_TOKEN(false),
    INTROSPECT_TOKEN_ERROR(false),

    FEDERATED_IDENTITY_LINK(true),
    FEDERATED_IDENTITY_LINK_ERROR(true),
    REMOVE_FEDERATED_IDENTITY(true),
    REMOVE_FEDERATED_IDENTITY_ERROR(true),

    UPDATE_EMAIL(true),
    UPDATE_EMAIL_ERROR(true),
    UPDATE_PROFILE(true),
    UPDATE_PROFILE_ERROR(true),
    UPDATE_PASSWORD(true),
    UPDATE_PASSWORD_ERROR(true),
    UPDATE_TOTP(true),
    UPDATE_TOTP_ERROR(true),
    VERIFY_EMAIL(true),
    VERIFY_EMAIL_ERROR(true),

    REMOVE_TOTP(true),
    REMOVE_TOTP_ERROR(true),

    GRANT_CONSENT(true),
    GRANT_CONSENT_ERROR(true),
    UPDATE_CONSENT(true),
    UPDATE_CONSENT_ERROR(true),
    REVOKE_GRANT(true),
    REVOKE_GRANT_ERROR(true),

    SEND_VERIFY_EMAIL(true),
    SEND_VERIFY_EMAIL_ERROR(true),
    SEND_RESET_PASSWORD(true),
    SEND_RESET_PASSWORD_ERROR(true),
    SEND_IDENTITY_PROVIDER_LINK(true),
    SEND_IDENTITY_PROVIDER_LINK_ERROR(true),
    RESET_PASSWORD(true),
    RESET_PASSWORD_ERROR(true),

    RESTART_AUTHENTICATION(true),
    RESTART_AUTHENTICATION_ERROR(true),

    INVALID_SIGNATURE(false),
    INVALID_SIGNATURE_ERROR(false),
    REGISTER_NODE(false),
    REGISTER_NODE_ERROR(false),
    UNREGISTER_NODE(false),
    UNREGISTER_NODE_ERROR(false),

    USER_INFO_REQUEST(false),
    USER_INFO_REQUEST_ERROR(false),

    IDENTITY_PROVIDER_LINK_ACCOUNT(true),
    IDENTITY_PROVIDER_LINK_ACCOUNT_ERROR(true),
    IDENTITY_PROVIDER_LOGIN(false),
    IDENTITY_PROVIDER_LOGIN_ERROR(false),
    IDENTITY_PROVIDER_FIRST_LOGIN(true),
    IDENTITY_PROVIDER_FIRST_LOGIN_ERROR(true),
    IDENTITY_PROVIDER_POST_LOGIN(true),
    IDENTITY_PROVIDER_POST_LOGIN_ERROR(true),
    IDENTITY_PROVIDER_RESPONSE(false),
    IDENTITY_PROVIDER_RESPONSE_ERROR(false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN(false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR(false),
    IMPERSONATE(true),
    IMPERSONATE_ERROR(true),
    CUSTOM_REQUIRED_ACTION(true),
    CUSTOM_REQUIRED_ACTION_ERROR(true),
    EXECUTE_ACTIONS(true),
    EXECUTE_ACTIONS_ERROR(true),
    EXECUTE_ACTION_TOKEN(true),
    EXECUTE_ACTION_TOKEN_ERROR(true),

    CLIENT_INFO(false),
    CLIENT_INFO_ERROR(false),
    CLIENT_REGISTER(true),
    CLIENT_REGISTER_ERROR(true),
    CLIENT_UPDATE(true),
    CLIENT_UPDATE_ERROR(true),
    CLIENT_DELETE(true),
    CLIENT_DELETE_ERROR(true),

    CLIENT_INITIATED_ACCOUNT_LINKING(true),
    CLIENT_INITIATED_ACCOUNT_LINKING_ERROR(true),
    TOKEN_EXCHANGE(true),
    TOKEN_EXCHANGE_ERROR(true),

    PERMISSION_TOKEN(true),
    PERMISSION_TOKEN_ERROR(false);

    private boolean saveByDefault;

    EventType(boolean saveByDefault) {
        this.saveByDefault = saveByDefault;
    }

    /**
     * Determines whether this event is stored when the admin has not set a specific set of event types to save.
     * @return
     */
    public boolean isSaveByDefault() {
        return saveByDefault;
    }

}
