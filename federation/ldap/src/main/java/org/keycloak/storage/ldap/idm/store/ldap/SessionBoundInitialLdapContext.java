/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap.idm.store.ldap;

import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;

import org.keycloak.models.KeycloakSession;

/**
 * A {@link InitialLdapContext} that binds instances of this class with the {@link KeycloakSession} so that any resource
 * acquired during the session lifetime is closed when the session is closed.
 */
public final class SessionBoundInitialLdapContext extends InitialLdapContext {

    public SessionBoundInitialLdapContext(KeycloakSession session, Hashtable<?, ?> environment, Control[] connCtls) throws NamingException {
        super(environment, connCtls);
        session.enlistForClose(() -> {
                try {
                    close();
                } catch (NamingException e) {
                    failedToCloseLdapContext(e);
                }
        });
    }

    private void failedToCloseLdapContext(NamingException e) {
        throw new RuntimeException("Failed to close LDAP context", e);
    }
}
