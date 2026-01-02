/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.forms.login.freemarker.model;

import java.util.List;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.models.UserModel;

public class OrganizationAwareAuthenticationContextBean extends AuthenticationContextBean {

    private final AuthenticationContextBean delegate;
    private final boolean showTryAnotherWayLink;

    public OrganizationAwareAuthenticationContextBean(AuthenticationContextBean delegate, boolean showTryAnotherWayLink) {
        super(null, null);
        this.delegate = delegate;
        this.showTryAnotherWayLink = showTryAnotherWayLink;
    }

    @Override
    public List<AuthenticationSelectionOption> getAuthenticationSelections() {
        return delegate.getAuthenticationSelections();
    }

    public boolean showTryAnotherWayLink() {
        if (showTryAnotherWayLink) {
            return delegate.showTryAnotherWayLink();
        }
        return false;
    }

    public boolean showUsername() {
        return delegate.showUsername();
    }

    public boolean showResetCredentials() {
        return delegate.showResetCredentials();
    }

    @Override
    public String getAttemptedUsername() {
        if (delegate.getContext() == null) {
            return null;
        }
        
        AuthenticationFlowContext context = delegate.getContext();
        String username = context.getAuthenticationSession().getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);

        if (username == null) {
            // Fall back to delegate's behavior when no attempted username is stored
            return delegate.getAttemptedUsername();
        }
        
        UserModel user = context.getUser();
        if (user == null) {
            return username;
        }

        String userEmail = user.getEmail();
        String userUsername = user.getUsername();
        
        if (userEmail != null && userEmail.equalsIgnoreCase(username)) {
            // User entered their email - show what they entered
            return username;
        } else if (userUsername != null && userUsername.equalsIgnoreCase(username)) {
            // User entered their username - show what they entered
            return username;
        }

        return username;
    }
}
