package org.keycloak.userprofile.validation;

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
                .addValidationFunction("FAKE_FIELD_ERRORKEY", (value, updateUserProfileContext) -> !value.equals("content")).build()
                .addAttributeValidator().forAttribute("firstName")
                .addValidationFunction("FIRST_NAME_FIELD_ERRORKEY", (value, updateUserProfileContext) -> true).build();

        //default user content
        rep.singleAttribute(UserModel.FIRST_NAME, "firstName");
        rep.singleAttribute(UserModel.LAST_NAME, "lastName");
        rep.singleAttribute(UserModel.EMAIL, "email");
        rep.singleAttribute("FAKE_FIELD", "content");
        rep.singleAttribute("NULLABLE_FIELD", null);

        updateContext = DefaultUserProfileContext.forRegistrationProfile();

    }

    @Test
    public void validate() {
        testchain = builder.build();
        UserProfileValidationResult results = new UserProfileValidationResult(testchain.validate(updateContext, new UserRepresentationUserProfile(rep)));
        Assert.assertEquals(true, results.hasFailureOfErrorType("FAKE_FIELD_ERRORKEY"));
        Assert.assertEquals(false, results.hasFailureOfErrorType("FIRST_NAME_FIELD_ERRORKEY"));
        Assert.assertEquals(true, results.getValidationResults().stream().filter(o -> o.getField().equals("firstName")).collect(Collectors.toList()).get(0).isValid());
        Assert.assertEquals(2, results.getValidationResults().size());

    }

    @Test
    public void mergedConfig() {
        testchain = builder.addAttributeValidator().forAttribute("FAKE_FIELD")
                .addValidationFunction("FAKE_FIELD_ERRORKEY_1", (value, updateUserProfileContext) -> false).build()
                .addAttributeValidator().forAttribute("FAKE_FIELD")
                .addValidationFunction("FAKE_FIELD_ERRORKEY_2", (value, updateUserProfileContext) -> false).build().build();

        UserProfileValidationResult results = new UserProfileValidationResult(testchain.validate(updateContext, new UserRepresentationUserProfile(rep)));
        Assert.assertEquals(true, results.hasFailureOfErrorType("FAKE_FIELD_ERRORKEY_1"));
        Assert.assertEquals(true, results.hasFailureOfErrorType("FAKE_FIELD_ERRORKEY_2"));
        Assert.assertEquals(true, results.getValidationResults().stream().filter(o -> o.getField().equals("firstName")).collect(Collectors.toList()).get(0).isValid());
        Assert.assertEquals(false, results.hasAttributeChanged("firstName"));

    }

    @Test
    public void emptyChain() {
        UserProfileValidationResult results = new UserProfileValidationResult(ValidationChainBuilder.builder().build().validate(updateContext,new UserRepresentationUserProfile(rep) ));
        Assert.assertEquals(Collections.emptyList(), results.getValidationResults());
    }
}
