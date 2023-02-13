/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.testsuite.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.Validators;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ValidatorTest extends AbstractTestRealmKeycloakTest {
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.user("alice");
    }

    @Test
    public void testDateValidator() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) ValidatorTest::testDateValidator);
    }

    private static void testDateValidator(KeycloakSession session) {
        assertTrue(Validators.dateValidator().validate(null, new ValidationContext(session)).isValid());
        assertTrue(Validators.dateValidator().validate("", new ValidationContext(session)).isValid());

        // defaults to Locale.ENGLISH as per default locale selector
        assertFalse(Validators.dateValidator().validate("13/12/2021", new ValidationContext(session)).isValid());
        assertFalse(Validators.dateValidator().validate("13/12/21", new ValidationContext(session)).isValid());
        assertTrue(Validators.dateValidator().validate("12/13/2021", new ValidationContext(session)).isValid());
        RealmModel realm = session.getContext().getRealm();

        realm.setInternationalizationEnabled(true);
        realm.setDefaultLocale(Locale.FRANCE.getLanguage());

        assertTrue(Validators.dateValidator().validate("13/12/21", new ValidationContext(session)).isValid());
        assertTrue(Validators.dateValidator().validate("13/12/2021", new ValidationContext(session)).isValid());
        assertFalse(Validators.dateValidator().validate("12/13/2021", new ValidationContext(session)).isValid());

        UserModel alice = session.users().getUserByUsername(realm, "alice");

        alice.setAttribute(UserModel.LOCALE, Collections.singletonList(Locale.ENGLISH.getLanguage()));

        ValidationContext context = new ValidationContext(session);

        context.getAttributes().put(UserModel.class.getName(), alice);

        assertFalse(Validators.dateValidator().validate("13/12/2021", context).isValid());
    }
}
