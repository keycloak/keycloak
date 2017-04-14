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
package org.keycloak.adapters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryParamterTokenRequestAuthenticatorTest {
    @Mock
    private KeycloakDeployment keycloakDeployment;
    @Mock
    private HttpFacade httpFacade;

    private QueryParamterTokenRequestAuthenticator queryParamterTokenRequestAuthenticator;

    @Before
    public void setup() {
        when(keycloakDeployment.getRealm()).thenReturn("realm");
        when(keycloakDeployment.isOAuthQueryParameterEnabled()).thenReturn(true);
        this.queryParamterTokenRequestAuthenticator = spy(new QueryParamterTokenRequestAuthenticator(keycloakDeployment));
    }

    @Test
    public void shouldReturnNotAttemptedWhenQueryParameterNotSet() {
        doReturn(null).when(queryParamterTokenRequestAuthenticator).getAccessTokenFromQueryParamter(httpFacade);

        AuthOutcome authOutcome = queryParamterTokenRequestAuthenticator.authenticate(httpFacade);

        assertEquals(authOutcome, AuthOutcome.NOT_ATTEMPTED);
    }

    @Test
    public void shouldReturnNotAttemptedWhenQueryParameterIsEmptyString() {
        doReturn("").when(queryParamterTokenRequestAuthenticator).getAccessTokenFromQueryParamter(httpFacade);

        AuthOutcome authOutcome = queryParamterTokenRequestAuthenticator.authenticate(httpFacade);

        assertEquals(authOutcome, AuthOutcome.NOT_ATTEMPTED);
        verify(queryParamterTokenRequestAuthenticator).getAccessTokenFromQueryParamter(httpFacade);
    }

    @Test
    public void shouldDelegateToAuthenticateTokenWhenAccessTokenSet() {
        final String accessToken = "test";
        final AuthOutcome expectedAuthOutcome = AuthOutcome.FAILED;
        doReturn(accessToken).when(queryParamterTokenRequestAuthenticator).getAccessTokenFromQueryParamter(httpFacade);
        doReturn(expectedAuthOutcome).when(queryParamterTokenRequestAuthenticator).authenticateToken(httpFacade, accessToken);

        AuthOutcome authOutcome = queryParamterTokenRequestAuthenticator.authenticate(httpFacade);

        assertEquals(authOutcome, expectedAuthOutcome);
        verify(queryParamterTokenRequestAuthenticator).authenticateToken(httpFacade, accessToken);
    }

    @Test
    public void shouldNotAttemptQueryParamterLogicWhenQueryParameterIsDisabled() {
        given(keycloakDeployment.isOAuthQueryParameterEnabled()).willReturn(false);

        AuthOutcome authOutcome = queryParamterTokenRequestAuthenticator.authenticate(httpFacade);

        verify(queryParamterTokenRequestAuthenticator, never()).getAccessTokenFromQueryParamter(any(HttpFacade.class));
        verify(queryParamterTokenRequestAuthenticator, never()).authenticateToken(any(HttpFacade.class), anyString());
        assertEquals(authOutcome, AuthOutcome.NOT_ATTEMPTED);
    }
}