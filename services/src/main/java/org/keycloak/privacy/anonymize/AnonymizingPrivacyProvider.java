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

import org.keycloak.events.Event;
import org.keycloak.privacy.PrivacyProvider;

/**
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public class AnonymizingPrivacyProvider implements PrivacyProvider {

    private final Anonymizer anonymizer;

    public AnonymizingPrivacyProvider(Anonymizer anonymizer) {
        this.anonymizer = anonymizer;
    }

    @Override
    public String filter(String type, String input) {
        return anonymizer.anonymize(type, input);
    }

    @Override
    public String filter(String type, String input, String key, Event event) {
        return anonymizer.anonymize(type, input);
    }

    @Override
    public String filter(String input) {
        return anonymizer.anonymize(Anonymizer.NULL, input);
    }
}
