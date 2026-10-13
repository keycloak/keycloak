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

package org.keycloak.testsuite.arquillian.migration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.testsuite.util.HttpClientUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrationContext {

    public static final Logger logger = Logger.getLogger(MigrationContext.class);

    private boolean runningMigrationTest = false;

    /**
     * @return true if testsuite is running migration test. This returns true during whole lifecycle of migration test (Including running of the old container as well as running the new container)
     */
    public boolean isRunningMigrationTest() {
        return runningMigrationTest;
    }

    public void setRunningMigrationTest(boolean runningMigrationTest) {
        this.runningMigrationTest = runningMigrationTest;
    }

    public String loadOfflineToken() throws Exception {
        String file = getOfflineTokenLocation();
        logger.infof("Reading previously saved offline token from the file: %s", file);

        try (FileInputStream fis = new FileInputStream(file)) {
            String offlineToken = StreamUtil.readString(fis, StandardCharsets.UTF_8);
            logger.infof("Successfully read offline token: %s", offlineToken);
            File f = new File(file);
            f.delete();
            logger.infof("Deleted file with offline token: %s", file);

            return offlineToken;
        }
    }


    // Do some actions on the old container
    public void runPreMigrationTask() throws Exception {
        String offlineToken = requestOfflineToken();
        saveOfflineToken(offlineToken);
    }

    private String requestOfflineToken() {
        logger.info("Requesting offline token on the old container");
        try (CloseableHttpClient httpClient = HttpClientUtils.createDefault()) {
            OAuthClient oauth = new OAuthClient(httpClient, null);
            oauth.init();
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            oauth.realm("Migration");
            oauth.client("migration-test-client", "secret");
            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("offline-test-user", "password2");
            return tokenResponse.getRefreshToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void saveOfflineToken(String offlineToken) throws Exception {
        String file = getOfflineTokenLocation();
        logger.infof("Saving offline token to file: %s, Offline token is: %s", file, offlineToken);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            writer.print(offlineToken);
        }
    }


    private String getOfflineTokenLocation() {
        String tmpDir = System.getProperty("java.io.tmpdir", "");
        if (tmpDir == null) {
            tmpDir = System.getProperty("basedir");
        }
        return tmpDir + "/offline-token.txt";
    }

}
