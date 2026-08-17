/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import static org.keycloak.models.Constants.KC_ACTION;
import static org.keycloak.models.Constants.KC_ACTION_STATUS;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractAppInitiatedActionTest extends AbstractActionsTest {

    protected static final String SUCCESS = "success";
    protected static final String CANCELLED = "cancelled";

    @InjectPage
    protected LoginPage loginPage;

    @InjectEvents
    protected Events events;

    protected abstract String getAiaAction();

    protected void doAIA() {
        oauth.loginForm().kcAction(getAiaAction()).open();
    }

    protected void assertKcActionStatus(String expectedStatus) {
        assertThat(oauth.parseLoginResponse().isSuccess(), is(true));
        String kcActionStatus = getCurrentUrlParam(KC_ACTION_STATUS);
        assertThat(kcActionStatus, is(expectedStatus));
    }

    protected void assertKcAction(String expectedKcAction) {
        assertThat(oauth.parseLoginResponse().isSuccess(), is(true));
        String kcAction = getCurrentUrlParam(KC_ACTION);
        assertThat(kcAction, is(expectedKcAction));
    }

    protected String getCurrentUrlParam(String paramName) {
        final URI url;
        try {
            url = new URI(this.driver.getCurrentUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        List<NameValuePair> pairs = URLEncodedUtils.parse(url, StandardCharsets.UTF_8);
        for (NameValuePair p : pairs) {
            if (p.getName().equals(paramName)) {
                return p.getValue();
            }
        }
        return null;
    }

    protected void assertSilentCancelMessage() {
        String url = this.driver.getCurrentUrl();
        assertThat("Expected no 'error=' in url", url, not(containsString("error=")));
        assertThat("Expected no 'error_description=' in url", url, not(containsString("error_description=")));
    }
}
