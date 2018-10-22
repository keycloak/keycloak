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

package org.keycloak.testsuite.ui.login;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.AbstractUiTest;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractLoginTest extends AbstractUiTest {
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation testRealmRep = testRealms.get(0);
        configureInternationalizationForRealm(testRealmRep);
    }

    protected void assertLoginFailed(String message) {
        assertCurrentUrlDoesntStartWith(testRealmAccountPage);
        assertTrue("Feedback message should be an error", loginPage.feedbackMessage().isError());
        assertEquals(message, loginPage.feedbackMessage().getText());
    }

    protected void assertLoginSuccessful() {
        assertCurrentUrlStartsWith(testRealmAccountPage);
    }
}
