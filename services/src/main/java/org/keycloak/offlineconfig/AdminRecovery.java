/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.offlineconfig;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.managers.ApplianceBootstrap;

/**
 * Static utility class that performs recovery on the master admin account.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class AdminRecovery {
    private static final Logger log = Logger.getLogger(AdminRecovery.class);

    public static final String RECOVER_ADMIN_ACCOUNT = "keycloak.recover-admin";
    public static final String TEMP_ADMIN_PASSWORD = "keycloak.temp-admin-password";

    // Don't allow instances
    private AdminRecovery() {}

    public static void recover(KeycloakSessionFactory sessionFactory) {
        if (!needRecovery()) return;

        KeycloakSession session = sessionFactory.create();

        session.getTransaction().begin();
        try {
            doRecover(session, getTempAdminPassword());
            session.getTransaction().commit();
            log.info("*******************************");
            log.info("Recovered Master Admin account.");
            log.info("*******************************");
        } finally {
            session.close();
            System.clearProperty(RECOVER_ADMIN_ACCOUNT);
            System.clearProperty(TEMP_ADMIN_PASSWORD);
        }
    }

    private static boolean needRecovery() {
        String strNeedRecovery = System.getProperty(RECOVER_ADMIN_ACCOUNT, "false");
        return Boolean.parseBoolean(strNeedRecovery);
    }

    private static String getTempAdminPassword() {
        String tempAdminPassword = System.getProperty(TEMP_ADMIN_PASSWORD);
        if ((tempAdminPassword == null) || tempAdminPassword.isEmpty()) {
            throw new OfflineConfigException("Must provide temporary admin password to recover admin account.");
        }
        return tempAdminPassword;
    }

    private static void doRecover(KeycloakSession session, String tempAdminPassword) {
        RealmProvider realmProvider = session.realms();
        UserProvider userProvider = session.users();

        String adminRealmName = Config.getAdminRealm();
        RealmModel realm = realmProvider.getRealmByName(adminRealmName);
        UserModel adminUser = userProvider.getUserByUsername("admin", realm);

        if (adminUser == null) {
            adminUser = userProvider.addUser(realm, "admin");
        }

        ApplianceBootstrap.setupAdminUser(session, realm, adminUser, tempAdminPassword);
    }
}
