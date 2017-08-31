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
package org.keycloak.testsuite.broker;

import org.junit.Before;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.social.facebook.FacebookIdentityProviderFactory;
import org.keycloak.social.github.GitHubIdentityProviderFactory;
import org.keycloak.social.paypal.PayPalIdentityProviderFactory;
import org.keycloak.social.google.GoogleIdentityProviderFactory;
import org.keycloak.social.linkedin.LinkedInIdentityProviderFactory;
import org.keycloak.social.stackoverflow.StackoverflowIdentityProviderFactory;
import org.keycloak.social.twitter.TwitterIdentityProviderFactory;
import org.keycloak.testsuite.model.AbstractModelTest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author pedroigor
 */
public abstract class AbstractIdentityProviderModelTest extends AbstractModelTest {

    private Set<String> expectedProviders;

    @Before
    public void onBefore() {
        this.expectedProviders = new HashSet<String>();

        this.expectedProviders.add(SAMLIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(OIDCIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(GoogleIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(FacebookIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(GitHubIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(PayPalIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(TwitterIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(LinkedInIdentityProviderFactory.PROVIDER_ID);
        this.expectedProviders.add(StackoverflowIdentityProviderFactory.PROVIDER_ID);

        this.expectedProviders = Collections.unmodifiableSet(this.expectedProviders);
    }

    protected Set<String> getExpectedProviders() {
        return this.expectedProviders;
    }
}
