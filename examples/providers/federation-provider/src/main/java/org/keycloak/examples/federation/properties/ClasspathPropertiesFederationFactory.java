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

package org.keycloak.examples.federation.properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserFederationProviderModel;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClasspathPropertiesFederationFactory extends BasePropertiesFederationFactory {

    public static final String PROVIDER_NAME = "classpath-properties";

    @Override
    protected BasePropertiesFederationProvider createProvider(KeycloakSession session, UserFederationProviderModel model, Properties props) {
        return new ClasspathPropertiesFederationProvider(session, model, props);
    }

    protected InputStream getPropertiesFileStream(String path) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Path not found for properties file");

        }
        return is;
    }



    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
