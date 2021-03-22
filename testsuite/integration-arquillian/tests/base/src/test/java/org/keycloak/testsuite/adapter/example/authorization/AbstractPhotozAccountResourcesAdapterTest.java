/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.example.authorization;

import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public abstract class AbstractPhotozAccountResourcesAdapterTest extends AbstractBasePhotozExampleAdapterTest {

    @Test
    public void testOwnerSharingResource() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);
        clientPage.accountShareResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);
        clientPage.accountShareRemoveScope(ALICE_ALBUM_NAME, "jdoe", "album:delete");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.accountRevokeResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
    }

    @Test
    public void testRequestResourceToOwner() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.accountGrantResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();

        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.accountGrantRemoveScope(ALICE_ALBUM_NAME, "jdoe", "album:delete");

        // get back to clientPage and init javascript adapter in order to navigate to accountPage again
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);
        clientPage.accountGrantResource(ALICE_ALBUM_NAME, "jdoe");

        // get back to clientPage and init javascript adapter in order to log out correctly
        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);


        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
    }

    @Test
    public void testCsrfGrantAccess() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        clientPage.accountMyResources();
        clientPage.executeScript("document.forms[0].stateChecker.value = 'invalid'");
        clientPage.grantResource(ALICE_ALBUM_NAME, "jdoe");
        clientPage.assertError();
    }

    @Test
    public void testCsrfRevokeResource() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);

        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasDenied);

        loginToClientPage(aliceUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);

        clientPage.accountGrantResource(ALICE_ALBUM_NAME, "jdoe");

        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(aliceUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.accountMyResource(ALICE_ALBUM_NAME);
        clientPage.executeScript("document.forms[0].stateChecker.value = 'invalid'");
        clientPage.revokeResource(ALICE_ALBUM_NAME, "jdoe");
        clientPage.assertError();
    }

    @Test
    public void testCrfCheckSharingResource() throws Exception {
        loginToClientPage(aliceUser);
        clientPage.createAlbum(ALICE_ALBUM_NAME, true);
        clientPage.accountMyResource(ALICE_ALBUM_NAME);

        clientPage.executeScript("document.forms['shareForm'].stateChecker.value = 'invalid'");
        clientPage.shareResource("jdoe");
        clientPage.assertError();

        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login()
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);

        loginToClientPage(aliceUser);
        clientPage.accountMyResource(ALICE_ALBUM_NAME);
        clientPage.shareResource("jdoe");

        clientPage.navigateTo();
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);
        loginToClientPage(jdoeUser);
        clientPage.viewAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
        clientPage.deleteAlbum(ALICE_ALBUM_NAME, this::assertWasNotDenied);
    }
}
