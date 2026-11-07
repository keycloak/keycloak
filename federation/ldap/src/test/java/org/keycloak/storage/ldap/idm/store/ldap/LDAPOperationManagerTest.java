/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.BasicControl;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.tracing.TracingProvider;

import io.opentelemetry.api.trace.Span;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LDAPOperationManagerTest {

    @Test
    public void missingPagedResponseControlFailsCompleteSearch() throws Exception {
        KeycloakSession session = mock(KeycloakSession.class);
        TracingProvider tracing = mock(TracingProvider.class);
        when(session.getProvider(TracingProvider.class)).thenReturn(tracing);
        when(tracing.startSpan(eq(LDAPOperationManager.class), eq("execute"))).thenReturn(Span.getInvalid());

        LDAPConfig config = mock(LDAPConfig.class);
        when(config.getUuidLDAPAttributeName()).thenReturn("entryUUID");

        LDAPQuery query = mock(LDAPQuery.class);
        LDAPQuery.PaginationContext pagination = mock(LDAPQuery.PaginationContext.class);
        LdapContext context = mock(LdapContext.class);
        when(query.getPaginationContext()).thenReturn(pagination);
        when(pagination.getLdapContext()).thenReturn(context);
        when(query.getLimit()).thenReturn(1);
        when(query.getReturningLdapAttributes()).thenReturn(Set.of("cn"));
        when(query.isRequireCompleteResults()).thenReturn(true);

        @SuppressWarnings("unchecked")
        NamingEnumeration<SearchResult> search = mock(NamingEnumeration.class);
        when(context.search(any(LdapName.class), anyString(), any(SearchControls.class))).thenReturn(search);
        when(search.hasMore()).thenReturn(true, false);
        when(search.next()).thenReturn(new SearchResult("cn=first", null, null));
        // A server may return unrelated controls while omitting the paging response control.
        when(context.getResponseControls()).thenReturn(new Control[] { new BasicControl("1.2.3.4") });

        Condition condition = mock(Condition.class);
        when(condition.toFilter()).thenReturn("(objectClass=*)");

        try {
            new LDAPOperationManager(session, config, null)
                    .searchPaginated(new LdapName("ou=roles,dc=example,dc=org"), condition, query);
            Assert.fail("An incomplete paginated role search must fail");
        } catch (NamingException expected) {
            Assert.assertTrue(expected.getMessage().contains("Missing paged results response control"));
        }

        verify(search).close();
    }
}
