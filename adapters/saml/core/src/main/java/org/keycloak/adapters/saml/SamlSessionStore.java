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

package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.AdapterSessionStore;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlSessionStore extends AdapterSessionStore {
    public static final String CURRENT_ACTION = "SAML_CURRENT_ACTION";
    public static final String SAML_LOGIN_ERROR_STATUS = "SAML_LOGIN_ERROR_STATUS";
    public static final String SAML_LOGOUT_ERROR_STATUS = "SAML_LOGOUT_ERROR_STATUS";

    enum CurrentAction {
        NONE,
        LOGGING_IN,
        LOGGING_OUT
    }
    void setCurrentAction(CurrentAction action);
    boolean isLoggingIn();
    boolean isLoggingOut();

    boolean isLoggedIn();
    SamlSession getAccount();
    void saveAccount(SamlSession account);
    String getRedirectUri();
    void logoutAccount();
    void logoutByPrincipal(String principal);
    void logoutBySsoId(List<String> ssoIds);

}
