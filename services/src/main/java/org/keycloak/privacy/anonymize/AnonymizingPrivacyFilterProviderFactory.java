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
package org.keycloak.privacy.anonymize;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.privacy.PrivacyFilterProvider;
import org.keycloak.privacy.PrivacyFilterProviderFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import static org.keycloak.privacy.anonymize.Anonymizer.EMAIL;
import static org.keycloak.privacy.anonymize.Anonymizer.IP_ADDRESS;
import static org.keycloak.privacy.anonymize.Anonymizer.MOBILE;
import static org.keycloak.privacy.anonymize.Anonymizer.NULL;
import static org.keycloak.privacy.anonymize.Anonymizer.PHONE_NUMBER;
import static org.keycloak.privacy.anonymize.Anonymizer.USERNAME;
import static org.keycloak.privacy.anonymize.Anonymizer.USER_ID;

/**
 * {@link PrivacyFilterProviderFactory} for {@link AnonymizingPrivacyFilterProvider}.
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public class AnonymizingPrivacyFilterProviderFactory implements PrivacyFilterProviderFactory {

    public static final String ID = "anonymize";

    public static final String DEFAULT_FIELDS;

    static {
        StringJoiner joiner = new StringJoiner(",");
        Arrays.asList(USER_ID, IP_ADDRESS, USERNAME, EMAIL, PHONE_NUMBER, MOBILE, NULL).forEach(joiner::add);
        DEFAULT_FIELDS = joiner.toString();
    }

    protected volatile PrivacyFilterProvider provider;

    @Override
    public PrivacyFilterProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public void init(Config.Scope config) {

        if (provider != null) {
            return;
        }

        provider = createProvider(config);
    }

    protected AnonymizingPrivacyFilterProvider createProvider(Config.Scope config) {
        return new AnonymizingPrivacyFilterProvider(createAnonymizer(config));
    }

    protected Anonymizer createAnonymizer(Config.Scope config) {

        int minLength = config.getInt("minLength", 6);
        int prefixLength = config.getInt("prefixLength", 2);
        int suffixLength = config.getInt("suffixLength", 3);
        String placeHolder = config.get("placeHolder", "%");

        String fieldList = config.get("fields", DEFAULT_FIELDS);

        //  field that should be used if no field is provided
        String fallbackField = config.get("fallbackField");
        Set<String> fields = toFieldSet(fieldList);

        return new DefaultAnonymizer(minLength, prefixLength, suffixLength, placeHolder, fields, fallbackField);
    }

    protected Set<String> toFieldSet(String fieldList) {
        Set<String> fields = new HashSet<>();
        if (fieldList != null && !fieldList.trim().isEmpty()) {
            fields.addAll(Arrays.asList(fieldList.split(",")));
        }
        return fields;
    }

    @Override
    public String getId() {
        return ID;
    }
}
