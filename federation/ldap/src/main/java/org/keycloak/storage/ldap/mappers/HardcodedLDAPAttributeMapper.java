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

package org.keycloak.storage.ldap.mappers;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HardcodedLDAPAttributeMapper extends AbstractLDAPStorageMapper {

    private static final Logger logger = Logger.getLogger(HardcodedLDAPAttributeMapper.class);

    public static final String LDAP_ATTRIBUTE_NAME = "ldap.attribute.name";

    public static final String LDAP_ATTRIBUTE_VALUE = "ldap.attribute.value";

    public static Pattern substitution = Pattern.compile("\\$\\{([^}]+)\\}");

    public HardcodedLDAPAttributeMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
    }


    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        String ldapAttrName = mapperModel.get(LDAP_ATTRIBUTE_NAME);
        String ldapAttrValue = mapperModel.get(LDAP_ATTRIBUTE_VALUE);

        String computedValue = computeAttributeValue(ldapAttrName, ldapAttrValue, ldapUser, localUser, realm);

        ldapUser.setAttribute(ldapAttrName, Collections.singleton(computedValue));
    }


    protected String computeAttributeValue(String ldapAttrName, String ldapAttrValue, LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        Matcher m = substitution.matcher(ldapAttrValue);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String token = m.group(1);
            if (token.equals("RANDOM")) {
                String randomVal = getRandomValue();
                m.appendReplacement(sb, randomVal);
            } else {
                m.appendReplacement(sb, token);
            }
        }

        m.appendTail(sb);

        return sb.toString();
    }


    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVW1234567890";

    // Generate random character of length 30. Allowed chars are from range 33-126
    protected String getRandomValue() {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            char c = CHARS.charAt(r.nextInt(CHARS.length()));
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {

    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        // Don't update attribute in LDAP later. It's supposed to be written just at registration time
        String ldapAttrName = mapperModel.get(LDAP_ATTRIBUTE_NAME);
        ldapUser.addReadOnlyAttributeName(ldapAttrName);

        return delegate;
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {

    }
}
