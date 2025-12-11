/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.error;

import java.net.URI;
import java.util.List;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.pages.ErrorPage;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class EscapeErrorPageTest extends AbstractKeycloakTest {

    @Page
    public ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void innerScript() {
        checkMessage("\"<img src=<script>alert(1)</script>/>\"", "\"alert(1)/>\"");
    }

    @Test
    public void innerURL() {
        checkMessage("\"See https://www.keycloak.org/docs\"", "\"See https://www.keycloak.org/docs\"");
    }

    @Test
    public void URL() {
        checkMessage("See https://www.keycloak.org/docs", "See https://www.keycloak.org/docs");
    }

    @Test
    public void ampersandEscape() {
        checkMessage("&lt;img src=&quot;something&quot;&gt;", "");
    }

    @Test
    public void hexEscape() {
        checkMessage("&#x3C;img src&#61;something&#x2F;&#x3E;", "");
    }

    @Test
    public void plainText() {
        checkMessage("It doesn't work", "It doesn't work");
    }

    @Test
    public void textWithPlus() {
        checkMessage("Fact: 1+1=2", "Fact: 1+1=2");
    }

    private void checkMessage(String queryParam, String expected) {
        try {
            final URI uri = KeycloakUriBuilder.fromUri(suiteContext.getAuthServerInfo().getContextRoot().toURI())
                    .path("/auth/realms/master/testing/display-error-message")
                    .queryParam("message", queryParam)
                    .build();

            driver.navigate().to(uri.toURL());
            errorPage.assertCurrent();
            assertThat(errorPage.getError(), CoreMatchers.is(expected));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
