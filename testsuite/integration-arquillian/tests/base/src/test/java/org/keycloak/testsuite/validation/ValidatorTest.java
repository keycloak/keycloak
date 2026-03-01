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

import java.util.Collections;
import java.util.Locale;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.validate.BuiltinValidators;
import org.keycloak.validate.ValidationContext;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ValidatorTest extends AbstractTestRealmKeycloakTest {
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.user("alice");
    }

    @Test
    public void testLocalDateValidator() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) ValidatorTest::testLocalDateValidator);
    }

    @Test
    public void testIsoDateValidator() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) ValidatorTest::testIsoDateValidator);
    }

    private static void testLocalDateValidator(KeycloakSession session) {
        assertTrue(BuiltinValidators.dateValidator().validate(null, new ValidationContext(session)).isValid());
        assertTrue(BuiltinValidators.dateValidator().validate("", new ValidationContext(session)).isValid());

        // defaults to Locale.ENGLISH as per default locale selector
        assertFalse(BuiltinValidators.dateValidator().validate("13/12/2021", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.dateValidator().validate("13/12/21", new ValidationContext(session)).isValid());
        assertTrue(BuiltinValidators.dateValidator().validate("12/13/21", new ValidationContext(session)).isValid());
        assertTrue(BuiltinValidators.dateValidator().validate("12/13/2021", new ValidationContext(session)).isValid());
        RealmModel realm = session.getContext().getRealm();

        realm.setInternationalizationEnabled(true);
        realm.setDefaultLocale(Locale.FRANCE.getLanguage());

        assertTrue(BuiltinValidators.dateValidator().validate("13/12/21", new ValidationContext(session)).isValid());
        assertTrue(BuiltinValidators.dateValidator().validate("13/12/2021", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.dateValidator().validate("12/13/2021", new ValidationContext(session)).isValid());

        UserModel alice = session.users().getUserByUsername(realm, "alice");

        alice.setAttribute(UserModel.LOCALE, Collections.singletonList(Locale.ENGLISH.getLanguage()));

        ValidationContext context = new ValidationContext(session);

        context.getAttributes().put(UserModel.class.getName(), alice);

        assertFalse(BuiltinValidators.dateValidator().validate("13/12/2021", context).isValid());
    }

    private static void testIsoDateValidator(KeycloakSession session) {
        assertTrue(BuiltinValidators.isoDateValidator().validate(null, new ValidationContext(session)).isValid());
        assertTrue(BuiltinValidators.isoDateValidator().validate("", new ValidationContext(session)).isValid());
        assertTrue(BuiltinValidators.isoDateValidator().validate("2021-12-13", new ValidationContext(session)).isValid());

        assertFalse(BuiltinValidators.isoDateValidator().validate("13/12/2021", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.isoDateValidator().validate("13/12/21", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.isoDateValidator().validate("12/13/21", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.isoDateValidator().validate("13.12.21", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.isoDateValidator().validate("13.12.2021", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.isoDateValidator().validate("2021-13-12", new ValidationContext(session)).isValid());
        assertFalse(BuiltinValidators.isoDateValidator().validate("21-13-12", new ValidationContext(session)).isValid());
    }
}
