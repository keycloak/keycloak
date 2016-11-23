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

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class BasePropertiesStorageFactory<T extends BasePropertiesStorageProvider> implements UserStorageProviderFactory<T> {
    protected ConcurrentHashMap<String, Properties> files = new ConcurrentHashMap<String, Properties>();


    @Override
    public T create(KeycloakSession session, ComponentModel model) {
        // first get the path to our properties file from the stored configuration of this provider instance.
        String path = model.getConfig().getFirst("path");
        if (path == null) {
            throw new IllegalStateException("Path attribute not configured for provider");
        }
        // see if we already loaded the config file
        Properties props = files.get(path);
        if (props != null) return (T)createProvider(session, new UserStorageProviderModel(model), props);


        props = new Properties();
        InputStream is = getPropertiesFileStream(path);
        if (is != null) {
            try {
                props.load(is);
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // remember the properties file for next time
        files.put(path, props);
        return (T)createProvider(session, new UserStorageProviderModel(model), props);
    }

    protected abstract InputStream getPropertiesFileStream(String path);

    protected abstract BasePropertiesStorageProvider createProvider(KeycloakSession session, UserStorageProviderModel model, Properties props);

}
