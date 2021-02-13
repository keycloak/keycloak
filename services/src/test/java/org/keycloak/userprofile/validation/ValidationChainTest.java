/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.userprofile.validation;

import static org.keycloak.userprofile.profile.UserProfileContextFactory.forProfile;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.userprofile.profile.DefaultUserProfileContext;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.profile.representations.UserRepresentationUserProfile;

import java.util.Collections;
import java.util.stream.Collectors;

public class ValidationChainTest {

    ValidationChainBuilder builder;
    ValidationChain testchain;
    UserProfile user;
    DefaultUserProfileContext updateContext;
    UserRepresentation rep = new UserRepresentation();

    @Before
    public void setUp() throws Exception {
        builder = ValidationChainBuilder.builder()
                .addAttributeValidator().forAttribute("FAKE_FIELD")
                .addSingleAttributeValueValidationFunction("FAKE_FIELD_ERRORKEY", (value, updateUserProfileContext) -> !value.equals("content")).build()
                .addAttributeValidator().forAttribute("firstName")
                .addSingleAttributeValueValidationFunction("FIRST_NAME_FIELD_ERRORKEY", (value, updateUserProfileContext) -> true).build();

        //default user content
        rep.singleAttribute(UserModel.FIRST_NAME, "firstName");
        rep.singleAttribute(UserModel.LAST_NAME, "lastName");
        rep.singleAttribute(UserModel.EMAIL, "email");
        rep.singleAttribute("FAKE_FIELD", "content");
        rep.singleAttribute("NULLABLE_FIELD", null);

        updateContext = forProfile(UserUpdateEvent.RegistrationProfile);

    }

    @Test
    public void validate() {
        testchain = builder.build();
        UserProfileValidationResult results = new UserProfileValidationResult(testchain.validate(updateContext, new UserRepresentationUserProfile(rep)), null);
        Assert.assertEquals(true, results.hasFailureOfErrorType("FAKE_FIELD_ERRORKEY"));
        Assert.assertEquals(false, results.hasFailureOfErrorType("FIRST_NAME_FIELD_ERRORKEY"));
        Assert.assertEquals(true, results.getValidationResults().stream().filter(o -> o.getField().equals("firstName")).collect(Collectors.toList()).get(0).isValid());
        Assert.assertEquals(2, results.getValidationResults().size());

    }

    @Test
    public void mergedConfig() {
        testchain = builder.addAttributeValidator().forAttribute("FAKE_FIELD")
                .addSingleAttributeValueValidationFunction("FAKE_FIELD_ERRORKEY_1", (value, updateUserProfileContext) -> false).build()
                .addAttributeValidator().forAttribute("FAKE_FIELD")
                .addSingleAttributeValueValidationFunction("FAKE_FIELD_ERRORKEY_2", (value, updateUserProfileContext) -> false).build().build();

        UserProfileValidationResult results = new UserProfileValidationResult(testchain.validate(updateContext, new UserRepresentationUserProfile(rep)), null);
        Assert.assertEquals(true, results.hasFailureOfErrorType("FAKE_FIELD_ERRORKEY_1"));
        Assert.assertEquals(true, results.hasFailureOfErrorType("FAKE_FIELD_ERRORKEY_2"));
        Assert.assertEquals(true, results.getValidationResults().stream().filter(o -> o.getField().equals("firstName")).collect(Collectors.toList()).get(0).isValid());
        Assert.assertEquals(true, results.hasAttributeChanged("firstName"));

    }

    @Test
    public void emptyChain() {
        UserProfileValidationResult results = new UserProfileValidationResult(ValidationChainBuilder.builder().build().validate(updateContext,new UserRepresentationUserProfile(rep) ), null);
        Assert.assertEquals(Collections.emptyList(), results.getValidationResults());
    }
}
