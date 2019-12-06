/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.services.util.AuthenticationFlowHistoryHelper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationContextBean {

    private final AuthenticationFlowContext context;
    private final URI actionUri;

    public AuthenticationContextBean(AuthenticationFlowContext context, URI actionUri) {
        this.context = context;
        this.actionUri = actionUri;
    }


    public List<AuthenticationSelectionOption> getAuthenticationSelections() {
        return context==null ? Collections.emptyList() : context.getAuthenticationSelections();
    }

    public String getSelectedCredential() {
        return context==null ? null : context.getSelectedCredentialId();
    }

    public boolean showBackButton() {
        if (context == null) {
            return false;
        }

        return actionUri != null && new AuthenticationFlowHistoryHelper(context.getAuthenticationSession(), context.getFlowPath()).hasAnyExecution();
    }
}
