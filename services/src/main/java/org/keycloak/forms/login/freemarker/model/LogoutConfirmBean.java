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
 *
 */

package org.keycloak.forms.login.freemarker.model;

import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LogoutConfirmBean {

    private final String code;
    private final AuthenticationSessionModel logoutSession;

    public LogoutConfirmBean(String code, AuthenticationSessionModel logoutSession) {
        this.code = code;
        this.logoutSession = logoutSession;
    }

    public String getCode() {
        return code;
    }

    public boolean isSkipLink() {
        return logoutSession == null || logoutSession.getClient().equals(SystemClientUtil.getSystemClient(logoutSession.getRealm()));
    }
}
