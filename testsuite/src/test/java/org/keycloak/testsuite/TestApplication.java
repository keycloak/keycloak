/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.SaasService;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TestApplication extends KeycloakApplication {

    public TestApplication(@Context ServletContext servletContext) {
        super(servletContext);
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();
        RealmManager realmManager = new RealmManager(session);
        if (realmManager.defaultRealm() == null) {
            install(realmManager);
        }
        session.getTransaction().commit();
    }

    public void install(RealmManager manager) {
        RealmModel defaultRealm = manager.createRealm(RealmModel.DEFAULT_REALM, RealmModel.DEFAULT_REALM);
        defaultRealm.setName(RealmModel.DEFAULT_REALM);
        defaultRealm.setEnabled(true);
        defaultRealm.setTokenLifespan(300);
        defaultRealm.setAccessCodeLifespan(60);
        defaultRealm.setSslNotRequired(true);
        defaultRealm.setCookieLoginAllowed(true);
        defaultRealm.setRegistrationAllowed(true);
        manager.generateRealmKeys(defaultRealm);
        defaultRealm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        defaultRealm.addRole(SaasService.REALM_CREATOR_ROLE);
        defaultRealm.addDefaultRole(SaasService.REALM_CREATOR_ROLE);

        RealmRepresentation rep = loadJson("META-INF/testrealm.json");
        RealmModel realm = manager.createRealm("demo", rep.getRealm());
        manager.importRealm(rep, realm);

    }

    public static RealmRepresentation loadJson(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        try {
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            // System.out.println(new String(bytes));

            return JsonSerialization.fromBytes(RealmRepresentation.class, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
