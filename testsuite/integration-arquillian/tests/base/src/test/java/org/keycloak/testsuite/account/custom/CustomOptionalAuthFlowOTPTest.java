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
package org.keycloak.testsuite.account.custom;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.PageUtils;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.*;
import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 * Test for the voluntary & conditional OTP Functionality.
 *
 * @author <a href="mailto:thorsten.pohl@lht.dlh.de">Thorsten Pohl</a>
 */
public class CustomOptionalAuthFlowOTPTest extends AbstractCustomAuthFlowOTPTest {

    @Before
    @Override
    public void beforeTest() {
        super.beforeTest();
    }


    @Test
    public void conditionalOTPDefaultOptionalOTPisConfigured() {
        // Yes we configured OTP voluntarily
        configureRequiredActions();
        configureOTP();

        //prepare config - default optional
        Map<String, String> config = new HashMap<>();
        config.put(DEFAULT_OTP_OUTCOME, OPTIONAL);

        setConditionalOTPForm(config);

        testRealmLoginPage.form().login(testUser);

        //verify that the page is login page, not totp setup
        assertCurrentUrlStartsWith(testLoginOneTimeCodePage);
    }

    @Test
    public void conditionalOTPDefaultOptionalOTPisNotConfigured() {
        //prepare config - default optional
        Map<String, String> config = new HashMap<>();
        config.put(DEFAULT_OTP_OUTCOME, OPTIONAL);

        setConditionalOTPForm(config);

        // Notice we have NOT configured OTP voluntarily

        //test OTP is skipped
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);
    }
    
}
