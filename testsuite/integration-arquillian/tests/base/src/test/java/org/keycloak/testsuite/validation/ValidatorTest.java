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

import jakarta.ws.rs.core.Response;
import org.junit.Test;
import org.keycloak.models.*;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.validate.BuiltinValidators;
import org.keycloak.validate.ValidationContext;

import java.util.*;

import static org.junit.Assert.*;

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
    public void testEmailExistsAsUsernameValidator(){
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) ValidatorTest::testEmailAsUsernameValidator);
    }
    @Test
    public void testUsernameAsEmailValidator(){
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) ValidatorTest::testUsernameAsEmailValidator);
    }
    @Test
    public void testIsoDateValidator() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) ValidatorTest::testIsoDateValidator);
    }


    private static UserProfile getUserProfile(String username,String email,KeycloakSession session){
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
        UserRepresentation userRepresentation=new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setEmail(email);
        userRepresentation.setId(UUID.randomUUID().toString());
        return profileProvider.create(UserProfileContext.USER_API,userRepresentation.getRawAttributes());
    }

    private static void testUsernameAsEmailValidator(KeycloakSession session){
        String username="Mario@example.com";
        String email="Mariolino@example.com";
        String username2=email;
        String email2="prova123@example.com";

        RealmModel realm=session.getContext().getRealm();
        UserProfile userProfile1=getUserProfile(username,email,session);
        UserModel userModel=userProfile1.create();
        UserProfile userProfile2=getUserProfile(username2,email2,session);
        List<ValidationException.Error> errors=null;
        try {
            userProfile2.validate();
        }catch (ValidationException validationException){
            errors=new ArrayList<>(validationException.getErrors());
        }
        boolean foundError=false;
        if(errors!=null){
            for(ValidationException.Error singolo:errors){
                if(singolo.getMessage().equals(Messages.USERNAME_ALREADY_USED_AS_EMAIL)){
                    foundError=true;
                    break;
                }
            }
        }
        new UserManager(session).removeUser(realm,userModel);
        if(!foundError) fail("UsernameAlreadyUsedAsEmailValidator doesn't works correctly");
    }


    private static void testEmailAsUsernameValidator(KeycloakSession session){
        String username="Pasquale@example.com";
        String email="Pasquale888@example.com";
        String username2="Antonio";
        String email2=username;
        RealmModel realm=session.getContext().getRealm();
        UserProfile userProfile1=getUserProfile(username,email,session);
        UserModel userModel=userProfile1.create();
        UserProfile userProfile2=getUserProfile(username2,email2,session);
        List<ValidationException.Error> errors=null;
        try {
            userProfile2.validate();
        }catch (ValidationException validationException){
            errors=new ArrayList<>(validationException.getErrors());
        }
        boolean foundError=false;
        if(errors!=null){
            for(ValidationException.Error singolo:errors){
                if((!realm.isRegistrationEmailAsUsername() && singolo.getMessage().equals(Messages.EMAIL_ALREADY_USED_AS_USERNAME))){
                    foundError=true;
                    break;
                }
            }
        }
        new UserManager(session).removeUser(realm,userModel);

        if(!foundError ){
            fail("EmailExistsAsUsernameValidator doesn't works correctly");
        }
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
