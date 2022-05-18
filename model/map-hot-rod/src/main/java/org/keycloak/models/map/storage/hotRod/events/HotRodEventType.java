/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.events;

import org.infinispan.protostream.annotations.ProtoEnumValue;

public enum HotRodEventType {

    @ProtoEnumValue(number = 0)
    LOGIN,
    @ProtoEnumValue(number = 1)
    LOGIN_ERROR,
    @ProtoEnumValue(number = 2)
    REGISTER,
    @ProtoEnumValue(number = 3)
    REGISTER_ERROR,
    @ProtoEnumValue(number = 4)
    LOGOUT,
    @ProtoEnumValue(number = 5)
    LOGOUT_ERROR,
    @ProtoEnumValue(number = 6)
    CODE_TO_TOKEN,
    @ProtoEnumValue(number = 7)
    CODE_TO_TOKEN_ERROR,
    @ProtoEnumValue(number = 8)
    CLIENT_LOGIN,
    @ProtoEnumValue(number = 9)
    CLIENT_LOGIN_ERROR,
    @ProtoEnumValue(number = 10)
    REFRESH_TOKEN,
    @ProtoEnumValue(number = 11)
    REFRESH_TOKEN_ERROR,
    @ProtoEnumValue(number = 12)
    /**
     * @deprecated see KEYCLOAK-2266
     */
    @Deprecated
    VALIDATE_ACCESS_TOKEN,
    @ProtoEnumValue(number = 13)
    @Deprecated
    VALIDATE_ACCESS_TOKEN_ERROR,
    @ProtoEnumValue(number = 14)
    INTROSPECT_TOKEN,
    @ProtoEnumValue(number = 15)
    INTROSPECT_TOKEN_ERROR,
    @ProtoEnumValue(number = 16)
    FEDERATED_IDENTITY_LINK,
    @ProtoEnumValue(number = 17)
    FEDERATED_IDENTITY_LINK_ERROR,
    @ProtoEnumValue(number = 18)
    REMOVE_FEDERATED_IDENTITY,
    @ProtoEnumValue(number = 19)
    REMOVE_FEDERATED_IDENTITY_ERROR,
    @ProtoEnumValue(number = 20)
    UPDATE_EMAIL,
    @ProtoEnumValue(number = 21)
    UPDATE_EMAIL_ERROR,
    @ProtoEnumValue(number = 22)
    UPDATE_PROFILE,
    @ProtoEnumValue(number = 23)
    UPDATE_PROFILE_ERROR,
    @ProtoEnumValue(number = 24)
    UPDATE_PASSWORD,
    @ProtoEnumValue(number = 25)
    UPDATE_PASSWORD_ERROR,
    @ProtoEnumValue(number = 26)
    UPDATE_TOTP,
    @ProtoEnumValue(number = 27)
    UPDATE_TOTP_ERROR,
    @ProtoEnumValue(number = 28)
    VERIFY_EMAIL,
    @ProtoEnumValue(number = 29)
    VERIFY_EMAIL_ERROR,
    @ProtoEnumValue(number = 30)
    VERIFY_PROFILE,
    @ProtoEnumValue(number = 31)
    VERIFY_PROFILE_ERROR,
    @ProtoEnumValue(number = 32)
    REMOVE_TOTP,
    @ProtoEnumValue(number = 33)
    REMOVE_TOTP_ERROR,
    @ProtoEnumValue(number = 34)
    GRANT_CONSENT,
    @ProtoEnumValue(number = 35)
    GRANT_CONSENT_ERROR,
    @ProtoEnumValue(number = 36)
    UPDATE_CONSENT,
    @ProtoEnumValue(number = 37)
    UPDATE_CONSENT_ERROR,
    @ProtoEnumValue(number = 38)
    REVOKE_GRANT,
    @ProtoEnumValue(number = 39)
    REVOKE_GRANT_ERROR,
    @ProtoEnumValue(number = 40)
    SEND_VERIFY_EMAIL,
    @ProtoEnumValue(number = 41)
    SEND_VERIFY_EMAIL_ERROR,
    @ProtoEnumValue(number = 42)
    SEND_RESET_PASSWORD,
    @ProtoEnumValue(number = 43)
    SEND_RESET_PASSWORD_ERROR,
    @ProtoEnumValue(number = 44)
    SEND_IDENTITY_PROVIDER_LINK,
    @ProtoEnumValue(number = 45)
    SEND_IDENTITY_PROVIDER_LINK_ERROR,
    @ProtoEnumValue(number = 46)
    RESET_PASSWORD,
    @ProtoEnumValue(number = 47)
    RESET_PASSWORD_ERROR,
    @ProtoEnumValue(number = 48)
    RESTART_AUTHENTICATION,
    @ProtoEnumValue(number = 49)
    RESTART_AUTHENTICATION_ERROR,
    @ProtoEnumValue(number = 50)
    INVALID_SIGNATURE,
    @ProtoEnumValue(number = 51)
    INVALID_SIGNATURE_ERROR,
    @ProtoEnumValue(number = 52)
    REGISTER_NODE,
    @ProtoEnumValue(number = 53)
    REGISTER_NODE_ERROR,
    @ProtoEnumValue(number = 54)
    UNREGISTER_NODE,
    @ProtoEnumValue(number = 55)
    UNREGISTER_NODE_ERROR,
    @ProtoEnumValue(number = 56)
    USER_INFO_REQUEST,
    @ProtoEnumValue(number = 57)
    USER_INFO_REQUEST_ERROR,
    @ProtoEnumValue(number = 58)
    IDENTITY_PROVIDER_LINK_ACCOUNT,
    @ProtoEnumValue(number = 59)
    IDENTITY_PROVIDER_LINK_ACCOUNT_ERROR,
    @ProtoEnumValue(number = 60)
    IDENTITY_PROVIDER_LOGIN,
    @ProtoEnumValue(number = 61)
    IDENTITY_PROVIDER_LOGIN_ERROR,
    @ProtoEnumValue(number = 62)
    IDENTITY_PROVIDER_FIRST_LOGIN,
    @ProtoEnumValue(number = 63)
    IDENTITY_PROVIDER_FIRST_LOGIN_ERROR,
    @ProtoEnumValue(number = 64)
    IDENTITY_PROVIDER_POST_LOGIN,
    @ProtoEnumValue(number = 65)
    IDENTITY_PROVIDER_POST_LOGIN_ERROR,
    @ProtoEnumValue(number = 66)
    IDENTITY_PROVIDER_RESPONSE,
    @ProtoEnumValue(number = 67)
    IDENTITY_PROVIDER_RESPONSE_ERROR,
    @ProtoEnumValue(number = 68)
    IDENTITY_PROVIDER_RETRIEVE_TOKEN,
    @ProtoEnumValue(number = 69)
    IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR,
    @ProtoEnumValue(number = 70)
    IMPERSONATE,
    @ProtoEnumValue(number = 71)
    IMPERSONATE_ERROR,
    @ProtoEnumValue(number = 72)
    CUSTOM_REQUIRED_ACTION,
    @ProtoEnumValue(number = 73)
    CUSTOM_REQUIRED_ACTION_ERROR,
    @ProtoEnumValue(number = 74)
    EXECUTE_ACTIONS,
    @ProtoEnumValue(number = 75)
    EXECUTE_ACTIONS_ERROR,
    @ProtoEnumValue(number = 76)
    EXECUTE_ACTION_TOKEN,
    @ProtoEnumValue(number = 77)
    EXECUTE_ACTION_TOKEN_ERROR,
    @ProtoEnumValue(number = 78)
    CLIENT_INFO,
    @ProtoEnumValue(number = 79)
    CLIENT_INFO_ERROR,
    @ProtoEnumValue(number = 80)
    CLIENT_REGISTER,
    @ProtoEnumValue(number = 81)
    CLIENT_REGISTER_ERROR,
    @ProtoEnumValue(number = 82)
    CLIENT_UPDATE,
    @ProtoEnumValue(number = 83)
    CLIENT_UPDATE_ERROR,
    @ProtoEnumValue(number = 84)
    CLIENT_DELETE,
    @ProtoEnumValue(number = 85)
    CLIENT_DELETE_ERROR,
    @ProtoEnumValue(number = 86)
    CLIENT_INITIATED_ACCOUNT_LINKING,
    @ProtoEnumValue(number = 87)
    CLIENT_INITIATED_ACCOUNT_LINKING_ERROR,
    @ProtoEnumValue(number = 88)
    TOKEN_EXCHANGE,
    @ProtoEnumValue(number = 89)
    TOKEN_EXCHANGE_ERROR,
    @ProtoEnumValue(number = 90)
    OAUTH2_DEVICE_AUTH,
    @ProtoEnumValue(number = 91)
    OAUTH2_DEVICE_AUTH_ERROR,
    @ProtoEnumValue(number = 92)
    OAUTH2_DEVICE_VERIFY_USER_CODE,
    @ProtoEnumValue(number = 93)
    OAUTH2_DEVICE_VERIFY_USER_CODE_ERROR,
    @ProtoEnumValue(number = 94)
    OAUTH2_DEVICE_CODE_TO_TOKEN,
    @ProtoEnumValue(number = 95)
    OAUTH2_DEVICE_CODE_TO_TOKEN_ERROR,
    @ProtoEnumValue(number = 96)
    AUTHREQID_TO_TOKEN,
    @ProtoEnumValue(number = 97)
    AUTHREQID_TO_TOKEN_ERROR,
    @ProtoEnumValue(number = 98)
    PERMISSION_TOKEN,
    @ProtoEnumValue(number = 99)
    PERMISSION_TOKEN_ERROR,
    @ProtoEnumValue(number = 100)
    DELETE_ACCOUNT,
    @ProtoEnumValue(number = 101)
    DELETE_ACCOUNT_ERROR,
    @ProtoEnumValue(number = 102)
    // PAR request.
    PUSHED_AUTHORIZATION_REQUEST,
    @ProtoEnumValue(number = 103)
    PUSHED_AUTHORIZATION_REQUEST_ERROR;
}
