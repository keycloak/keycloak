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

package org.keycloak.forms.login;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum LoginFormsPages {

    LOGIN, LOGIN_USERNAME, LOGIN_PASSWORD, LOGIN_TOTP, LOGIN_CONFIG_TOTP, LOGIN_WEBAUTHN, LOGIN_VERIFY_EMAIL,
    LOGIN_IDP_LINK_CONFIRM, LOGIN_IDP_LINK_EMAIL,
    OAUTH_GRANT, LOGIN_RESET_PASSWORD, LOGIN_UPDATE_PASSWORD, LOGIN_SELECT_AUTHENTICATOR, REGISTER, INFO, ERROR, ERROR_WEBAUTHN, LOGIN_UPDATE_PROFILE,
    LOGIN_PAGE_EXPIRED, CODE, X509_CONFIRM, SAML_POST_FORM;

}
