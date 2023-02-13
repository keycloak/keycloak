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
package org.keycloak.testsuite.user.profile.config;

import static org.keycloak.userprofile.config.UPConfigUtils.readConfig;
import static org.keycloak.userprofile.config.UPConfigUtils.validate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServer;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.userprofile.config.UPAttribute;
import org.keycloak.userprofile.config.UPAttributePermissions;
import org.keycloak.userprofile.config.UPAttributeRequired;
import org.keycloak.userprofile.config.UPConfig;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.userprofile.config.UPGroup;

/**
 * Unit test for {@link UPConfigUtils} functionality
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPConfigParserTest extends AbstractTestRealmKeycloakTest {

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {
	    testRealm.setClientScopes(new ArrayList<>());
	    testRealm.getClientScopes().add(ClientScopeBuilder.create().name("phone-1-sel").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("phone-1").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("phone-2-sel").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("phone-2").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("phone-3-sel").build());
	}
	
    @Test
    public void attributeNameIsValid() {
        // few invalid cases
        Assert.assertFalse(UPConfigUtils.isValidAttributeName(""));
        Assert.assertFalse(UPConfigUtils.isValidAttributeName(" "));
        Assert.assertFalse(UPConfigUtils.isValidAttributeName("a b"));
        Assert.assertFalse(UPConfigUtils.isValidAttributeName("a*b"));
        Assert.assertFalse(UPConfigUtils.isValidAttributeName("a%b"));
        Assert.assertFalse(UPConfigUtils.isValidAttributeName("a$b"));

        // few valid cases
        Assert.assertTrue(UPConfigUtils.isValidAttributeName("a-b"));
        Assert.assertTrue(UPConfigUtils.isValidAttributeName("a.b"));
        Assert.assertTrue(UPConfigUtils.isValidAttributeName("a_b"));
        Assert.assertTrue(UPConfigUtils.isValidAttributeName("a3B"));
    }

    @Test
    public void loadConfigurationFromJsonFile() throws IOException {
        UPConfig config = readConfig(getValidConfigFileIS());

        // only basic assertion to check config is loaded, more detailed tests follow
        Assert.assertEquals(5, config.getAttributes().size());
    }

    @Test
    public void parseConfigurationFile_OK() throws IOException {
        UPConfig config = loadValidConfig();

        Assert.assertNotNull(config);

        // assert *** attributes ***
        Assert.assertEquals(5, config.getAttributes().size());
        UPAttribute att = config.getAttributes().get(1);
        Assert.assertNotNull(att);
        Assert.assertEquals("email", att.getName());
        // validation
        Assert.assertEquals(3, att.getValidations().size());
        Assert.assertEquals(1, att.getValidations().get("length").size());
        Assert.assertEquals(255, att.getValidations().get("length").get("max"));
        // annotations
        Assert.assertEquals("userEmailFormFieldHint", att.getAnnotations().get("formHintKey"));

        att = config.getAttributes().get(4);
        // required
        Assert.assertNotNull(att.getRequired());
        Assert.assertFalse(att.getRequired().isAlways());
        Assert.assertNotNull(att.getRequired().getScopes());
        Assert.assertNotNull(att.getRequired().getRoles());
        Assert.assertEquals(2, att.getRequired().getRoles().size());
        
        att = config.getAttributes().get(3);
        Assert.assertTrue(att.getRequired().isAlways());
        
        // permissions
        Assert.assertNotNull(att.getPermissions());
        Assert.assertNotNull(att.getPermissions().getEdit());
        Assert.assertEquals(1, att.getPermissions().getEdit().size());
        Assert.assertTrue(att.getPermissions().getEdit().contains("admin"));
        Assert.assertNotNull(att.getPermissions().getView());
        Assert.assertEquals(2, att.getPermissions().getView().size());
        Assert.assertTrue(att.getPermissions().getView().contains("admin"));
        Assert.assertTrue(att.getPermissions().getView().contains("user"));
        
        //selector
        att = config.getAttributes().get(4);
        Assert.assertNotNull(att.getSelector().getScopes());
        Assert.assertEquals(3, att.getSelector().getScopes().size());
        Assert.assertTrue(att.getSelector().getScopes().contains("phone-3-sel"));
        
        //displayName
        att = config.getAttributes().get(4);
        Assert.assertEquals("${profile.phone}", att.getDisplayName());

        // group
        Assert.assertEquals("contact", att.getGroup());

        // assert *** groups ***
        Assert.assertEquals(1, config.getGroups().size());

        UPGroup group = config.getGroups().get(0);
        Assert.assertEquals("contact", group.getName());
        Assert.assertEquals("Contact information", group.getDisplayHeader());
        Assert.assertEquals("Required to contact you in case of emergency", group.getDisplayDescription());
        Assert.assertEquals(1, group.getAnnotations().size());
        Assert.assertEquals("value1", group.getAnnotations().get("contactanno1"));
    }

    /**
     * Parse valid JSON config from the test file for tests.
     * 
     * @return valid config
     * @throws IOException
     */
    private static UPConfig loadValidConfig() throws IOException {
        return readConfig(getValidConfigFileIS());
    }

    private static InputStream getValidConfigFileIS() {
        return UPConfigParserTest.class.getResourceAsStream("test-OK.json");
    }

    @Test(expected = JsonMappingException.class)
    public void parseConfigurationFile_invalidJsonFormat() throws IOException {
        readConfig(getClass().getResourceAsStream("test-invalidJsonFormat.json"));
    }

    @Test(expected = IOException.class)
    public void parseConfigurationFile_invalidType() throws IOException {
        readConfig(getClass().getResourceAsStream("test-invalidType.json"));
    }

    @Test(expected = IOException.class)
    public void parseConfigurationFile_unknownField() throws IOException {
        readConfig(getClass().getResourceAsStream("test-unknownField.json"));
    }

    @Test
    public void validateConfiguration_OK() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_OK);
    }

    public static void validateConfiguration_OK(KeycloakSession session) throws IOException {
        List<String> errors = validate(session, loadValidConfig());
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void validateConfiguration_attributeNameErrors() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_attributeNameErrors);
    }

    public static void validateConfiguration_attributeNameErrors(KeycloakSession session) throws IOException {
        UPConfig config = loadValidConfig();
        //we run this test without KeycloakSession so validator configs are not validated here

        UPAttribute attConfig = config.getAttributes().get(1);

        attConfig.setName(null);
        List<String> errors = validate(session, config);
        Assert.assertEquals(1, errors.size());

        attConfig.setName(" ");
        errors = validate(session, config);
        Assert.assertEquals(1, errors.size());

        // duplicate attribute name
        attConfig.setName("firstName");
        errors = validate(session, config);
        Assert.assertEquals(1, errors.size());

        // attribute name format error - unallowed character
        attConfig.setName("ema il");
        errors = validate(session, config);
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void validateConfiguration_attributePermissionsErrors() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_attributePermissionsErrors);
    }

    public static void validateConfiguration_attributePermissionsErrors(KeycloakSession session) throws IOException {
        UPConfig config = loadValidConfig();
        //we run this test without KeycloakSession so validator configs are not validated here
        
        UPAttribute attConfig = config.getAttributes().get(1);

        // no permissions configures at all
        attConfig.setPermissions(null);
        List<String> errors = validate(session, config);
        Assert.assertEquals(0, errors.size());

        // no permissions structure fields configured
        UPAttributePermissions permsConfig = new UPAttributePermissions();
        attConfig.setPermissions(permsConfig);
        errors = validate(session, config);
        Assert.assertTrue(errors.isEmpty());

        // valid if both are present, even empty
        permsConfig.setEdit(Collections.emptySet());
        permsConfig.setView(Collections.emptySet());
        attConfig.setPermissions(permsConfig);
        errors = validate(session, config);
        Assert.assertEquals(0, errors.size());

        Set<String> withInvRole = Collections.singleton("invalid");

        // invalid role used for view
        permsConfig.setView(withInvRole);
        errors = validate(session, config);
        Assert.assertEquals(1, errors.size());

        // invalid role used for edit also
        permsConfig.setEdit(withInvRole);
        errors = validate(session, config);
        Assert.assertEquals(2, errors.size());
    }

    @Test
    public void validateConfiguration_attributeRequirementsErrors() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_attributeRequirementsErrors);
    }

    public static void validateConfiguration_attributeRequirementsErrors(KeycloakSession session) throws IOException {
        UPConfig config = loadValidConfig();
        //we run this test without KeycloakSession so validator configs are not validated here
        
        UPAttribute attConfig = config.getAttributes().get(1);

        // it is OK without requirements configures at all
        attConfig.setRequired(null);
        List<String> errors = validate(session, config);
        Assert.assertEquals(0, errors.size());

        // it is OK with empty config as it means ALWAYS required
        UPAttributeRequired reqConfig = new UPAttributeRequired();
        attConfig.setRequired(reqConfig);
        errors = validate(session, config);
        Assert.assertEquals(0, errors.size());
        Assert.assertTrue(reqConfig.isAlways());

        // invalid role used
        reqConfig.setRoles(Collections.singleton("invalid"));
        errors = validate(session, config);
        Assert.assertEquals(1, errors.size());
        Assert.assertFalse(reqConfig.isAlways());

    }

    @Test
	public void validateConfiguration_attributeValidationsErrors() {
		getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_attributeValidationsErrors);
	}
    
    private static void validateConfiguration_attributeValidationsErrors(KeycloakSession session) throws IOException {
        UPConfig config = loadValidConfig();

        //reset all validations not to affect our test as they may be invalid  
        for(UPAttribute att: config.getAttributes()) {
        	att.setValidations(null);
        }
        
        //add validation config for one attribute for testing purposes
        Map<String, Map<String, Object>> validationConfig = new HashMap<>();
        config.getAttributes().get(1).setValidations(validationConfig);

        // empty validator name
        validationConfig.put(" ",null);
        List<String> errors = validate(session, config);
        Assert.assertEquals(1, errors.size());


        // wrong configuration for "length" validator
        validationConfig.clear();
        Map<String, Object> vc = new HashMap<>();
        vc.put("min", "aaa");
		validationConfig.put("length", vc );
        errors = validate(session, config);
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void validateConfiguration_attributeGroupConfigurationErrors() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_attributeGroupConfigurationErrors);
    }

    private static void validateConfiguration_attributeGroupConfigurationErrors(KeycloakSession session) throws IOException {
        UPConfig config = loadValidConfig();
        
        // add a group without name
        UPGroup groupWithoutName = new UPGroup();
        config.addGroup(groupWithoutName);
        List<String> errors = validate(session, config);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Name is mandatory for groups, found 1 group(s) without name.", errors.get(0));
    }

    @Test
    public void validateConfiguration_attributeGroupReferenceErrors() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_attributeGroupReferenceErrors);
    }

    private static void validateConfiguration_attributeGroupReferenceErrors(KeycloakSession session) throws IOException {
        UPConfig config = loadValidConfig();

        // attribute references group that is not configured
        UPAttribute firstAttribute = config.getAttributes().get(0);
        firstAttribute.setGroup("non-existing-group");
        List<String> errors = validate(session, config);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals("Attribute 'username' references unknown group 'non-existing-group'", errors.get(0));
    }
    
    @Test
    public void validateConfiguration_attributeAnnotationsErrors() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) UPConfigParserTest::validateConfiguration_attributeAnnotationsErrors);
    }

    private static void validateConfiguration_attributeAnnotationsErrors(KeycloakSession session) throws IOException {
        UPConfig config = loadValidConfig();

        // attribute references group that is not configured
        UPAttribute att = config.getAttributes().get(1);
        att.getAnnotations().put("inputOptions", "");
        att.getAnnotations().put("inputOptionLabels", "");
        
        List<String> errors = validate(session, config);
        Assert.assertEquals(2, errors.size());
    }

}
