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
package org.keycloak.testsuite.actions;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.util.JsonSerialization;

@EnableFeature(Profile.Feature.DECLARATIVE_USER_PROFILE)
public class RequiredActionUpdateEmailUserProfileTest extends RequiredActionUpdateEmailTest {

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {
		super.configureTestRealm(testRealm);
		VerifyProfileTest.enableDynamicUserProfile(testRealm);
	}

	@Test
	public void additionalEmailValidatorsAreTriggered() throws IOException {
		UPConfig genuineConfiguration = testRealm().users().userProfile().getConfiguration();

		VerifyProfileTest.setUserProfileConfiguration(testRealm(), "{\"attributes\": ["
																   + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\": {\"length\": { \"min\": 3, \"max\": 3 }}}"
																   + "]}");
		try {
			loginPage.open();

			loginPage.login("test-user@localhost", "password");

			updateEmailPage.assertCurrent();
			updateEmailPage.changeEmail("foo@example.org");
			updateEmailPage.assertCurrent();

			// assert that form holds submitted values during validation error
			Assert.assertEquals("foo@example.org", updateEmailPage.getEmail());
			Assert.assertEquals("Length must be between 3 and 3.", updateEmailPage.getEmailInputError());

			events.assertEmpty();
		} finally {
			VerifyProfileTest.setUserProfileConfiguration(testRealm(), JsonSerialization.writeValueAsString(genuineConfiguration));
		}

	}
}
