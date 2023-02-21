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

import java.util.Map;
import java.util.Objects;
import org.keycloak.util.EnumWithStableIndex;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum EventType implements EnumWithStableIndex {

    LOGIN(0, true),
    LOGIN_ERROR(0x10000 + LOGIN.getStableIndex(), true),
    REGISTER(1, true),
    REGISTER_ERROR(0x10000 + REGISTER.getStableIndex(), true),
    LOGOUT(2, true),
    LOGOUT_ERROR(0x10000 + LOGOUT.getStableIndex(), true),

    CODE_TO_TOKEN(3, true),
    CODE_TO_TOKEN_ERROR(0x10000 + CODE_TO_TOKEN.getStableIndex(), true),

    CLIENT_LOGIN(4, true),
    CLIENT_LOGIN_ERROR(0x10000 + CLIENT_LOGIN.getStableIndex(), true),

    REFRESH_TOKEN(5, false),
    REFRESH_TOKEN_ERROR(0x10000 + REFRESH_TOKEN.getStableIndex(), false),

    /**
     * @deprecated see KEYCLOAK-2266
     */
    @Deprecated
    VALIDATE_ACCESS_TOKEN(6, false),
    @Deprecated
    VALIDATE_ACCESS_TOKEN_ERROR(0x10000 + VALIDATE_ACCESS_TOKEN.getStableIndex(), false),
    INTROSPECT_TOKEN(7, false),
    INTROSPECT_TOKEN_ERROR(0x10000 + INTROSPECT_TOKEN.getStableIndex(), false),

    FEDERATED_IDENTITY_LINK(8, true),
    FEDERATED_IDENTITY_LINK_ERROR(0x10000 + FEDERATED_IDENTITY_LINK.getStableIndex(), true),
    REMOVE_FEDERATED_IDENTITY(9, true),
    REMOVE_FEDERATED_IDENTITY_ERROR(0x10000 + REMOVE_FEDERATED_IDENTITY.getStableIndex(), true),

    UPDATE_EMAIL(10, true),
    UPDATE_EMAIL_ERROR(0x10000 + UPDATE_EMAIL.getStableIndex(), true),
    UPDATE_PROFILE(11, true),
    UPDATE_PROFILE_ERROR(0x10000 + UPDATE_PROFILE.getStableIndex(), true),
    UPDATE_PASSWORD(12, true),
    UPDATE_PASSWORD_ERROR(0x10000 + UPDATE_PASSWORD.getStableIndex(), true),
    UPDATE_TOTP(13, true),
    UPDATE_TOTP_ERROR(0x10000 + UPDATE_TOTP.getStableIndex(), true),
    VERIFY_EMAIL(14, true),
    VERIFY_EMAIL_ERROR(0x10000 + VERIFY_EMAIL.getStableIndex(), true),
    VERIFY_PROFILE(15, true),
    VERIFY_PROFILE_ERROR(0x10000 + VERIFY_PROFILE.getStableIndex(), true),

    REMOVE_TOTP(16, true),
    REMOVE_TOTP_ERROR(0x10000 + REMOVE_TOTP.getStableIndex(), true),

    GRANT_CONSENT(17, true),
    GRANT_CONSENT_ERROR(0x10000 + GRANT_CONSENT.getStableIndex(), true),
    UPDATE_CONSENT(18, true),
    UPDATE_CONSENT_ERROR(0x10000 + UPDATE_CONSENT.getStableIndex(), true),
    REVOKE_GRANT(19, true),
    REVOKE_GRANT_ERROR(0x10000 + REVOKE_GRANT.getStableIndex(), true),

    SEND_VERIFY_EMAIL(20, true),
    SEND_VERIFY_EMAIL_ERROR(0x10000 + SEND_VERIFY_EMAIL.getStableIndex(), true),
    SEND_RESET_PASSWORD(21, true),
    SEND_RESET_PASSWORD_ERROR(0x10000 + SEND_RESET_PASSWORD.getStableIndex(), true),
    SEND_IDENTITY_PROVIDER_LINK(22, true),
    SEND_IDENTITY_PROVIDER_LINK_ERROR(0x10000 + SEND_IDENTITY_PROVIDER_LINK.getStableIndex(), true),
    RESET_PASSWORD(23, true),
    RESET_PASSWORD_ERROR(0x10000 + RESET_PASSWORD.getStableIndex(), true),

    RESTART_AUTHENTICATION(24, true),
    RESTART_AUTHENTICATION_ERROR(0x10000 + RESTART_AUTHENTICATION.getStableIndex(), true),

    INVALID_SIGNATURE(25, false),
    INVALID_SIGNATURE_ERROR(0x10000 + INVALID_SIGNATURE.getStableIndex(), false),
    REGISTER_NODE(26, false),
    REGISTER_NODE_ERROR(0x10000 + REGISTER_NODE.getStableIndex(), false),
    UNREGISTER_NODE(27, false),
    UNREGISTER_NODE_ERROR(0x10000 + UNREGISTER_NODE.getStableIndex(), false),

    USER_INFO_REQUEST(28, false),
    USER_INFO_REQUEST_ERROR(0x10000 + USER_INFO_REQUEST.getStableIndex(), false),

    IDENTITY_PROVIDER_LINK_ACCOUNT(29, true),
    IDENTITY_PROVIDER_LINK_ACCOUNT_ERROR(0x10000 + IDENTITY_PROVIDER_LINK_ACCOUNT.getStableIndex(), true),
    IDENTITY_PROVIDER_LOGIN(30, false),
    IDENTITY_PROVIDER_LOGIN_ERROR(0x10000 + IDENTITY_PROVIDER_LOGIN.getStableIndex(), false),
    IDENTITY_PROVIDER_FIRST_LOGIN(31, true),
    IDENTITY_PROVIDER_FIRST_LOGIN_ERROR(0x10000 + IDENTITY_PROVIDER_FIRST_LOGIN.getStableIndex(), true),
    IDENTITY_PROVIDER_POST_LOGIN(32, true),
    IDENTITY_PROVIDER_POST_LOGIN_ERROR(0x10000 + IDENTITY_PROVIDER_POST_LOGIN.getStableIndex(), true),
    IDENTITY_PROVIDER_RESPONSE(33, false),
    IDENTITY_PROVIDER_RESPONSE_ERROR(0x10000 + IDENTITY_PROVIDER_RESPONSE.getStableIndex(), false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN(34, false),
    IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR(0x10000 + IDENTITY_PROVIDER_RETRIEVE_TOKEN.getStableIndex(), false),
    IMPERSONATE(35, true),
    IMPERSONATE_ERROR(0x10000 + IMPERSONATE.getStableIndex(), true),
    CUSTOM_REQUIRED_ACTION(36, true),
    CUSTOM_REQUIRED_ACTION_ERROR(0x10000 + CUSTOM_REQUIRED_ACTION.getStableIndex(), true),
    EXECUTE_ACTIONS(37, true),
    EXECUTE_ACTIONS_ERROR(0x10000 + EXECUTE_ACTIONS.getStableIndex(), true),
    EXECUTE_ACTION_TOKEN(38, true),
    EXECUTE_ACTION_TOKEN_ERROR(0x10000 + EXECUTE_ACTION_TOKEN.getStableIndex(), true),

    CLIENT_INFO(39, false),
    CLIENT_INFO_ERROR(0x10000 + CLIENT_INFO.getStableIndex(), false),
    CLIENT_REGISTER(40, true),
    CLIENT_REGISTER_ERROR(0x10000 + CLIENT_REGISTER.getStableIndex(), true),
    CLIENT_UPDATE(41, true),
    CLIENT_UPDATE_ERROR(0x10000 + CLIENT_UPDATE.getStableIndex(), true),
    CLIENT_DELETE(42, true),
    CLIENT_DELETE_ERROR(0x10000 + CLIENT_DELETE.getStableIndex(), true),

    CLIENT_INITIATED_ACCOUNT_LINKING(43, true),
    CLIENT_INITIATED_ACCOUNT_LINKING_ERROR(0x10000 + CLIENT_INITIATED_ACCOUNT_LINKING.getStableIndex(), true),
    TOKEN_EXCHANGE(44, true),
    TOKEN_EXCHANGE_ERROR(0x10000 + TOKEN_EXCHANGE.getStableIndex(), true),

    OAUTH2_DEVICE_AUTH(45, true),
    OAUTH2_DEVICE_AUTH_ERROR(0x10000 + OAUTH2_DEVICE_AUTH.getStableIndex(), true),
    OAUTH2_DEVICE_VERIFY_USER_CODE(46, true),
    OAUTH2_DEVICE_VERIFY_USER_CODE_ERROR(0x10000 + OAUTH2_DEVICE_VERIFY_USER_CODE.getStableIndex(), true),
    OAUTH2_DEVICE_CODE_TO_TOKEN(47, true),
    OAUTH2_DEVICE_CODE_TO_TOKEN_ERROR(0x10000 + OAUTH2_DEVICE_CODE_TO_TOKEN.getStableIndex(), true),

    AUTHREQID_TO_TOKEN(48, true),
    AUTHREQID_TO_TOKEN_ERROR(0x10000 + AUTHREQID_TO_TOKEN.getStableIndex(), true),

    PERMISSION_TOKEN(49, true),
    PERMISSION_TOKEN_ERROR(0x10000 + PERMISSION_TOKEN.getStableIndex(), false),

    DELETE_ACCOUNT(50, true),
    DELETE_ACCOUNT_ERROR(0x10000 + DELETE_ACCOUNT.getStableIndex(), true),

    // PAR request.
    PUSHED_AUTHORIZATION_REQUEST(51, false),
    PUSHED_AUTHORIZATION_REQUEST_ERROR(0x10000 + PUSHED_AUTHORIZATION_REQUEST.getStableIndex(), false);


    private final int stableIndex;
    private final boolean saveByDefault;
    private static final Map<Integer, EventType> BY_ID = EnumWithStableIndex.getReverseIndex(values());

    EventType(int stableIndex, boolean saveByDefault) {
        Objects.requireNonNull(stableIndex);
        this.stableIndex = stableIndex;
        this.saveByDefault = saveByDefault;
    }

    @Override
    public int getStableIndex() {
        return stableIndex;
    }

    /**
     * Determines whether this event is stored when the admin has not set a specific set of event types to save.
     * @return
     */
    public boolean isSaveByDefault() {
        return saveByDefault;
    }

    public static EventType valueOfInteger(Integer id) {
        return id == null ? null : BY_ID.get(id);
    }
}
