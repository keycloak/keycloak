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

package org.keycloak.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.provider.Provider;

/**
 * A theme resource provider can be used to load additional templates and resources. An example use of this would be
 * a custom authenticator that requires an additional template and a JavaScript file.
 *
 * The theme is searched for templates and resources first. Theme resource providers are only searched if the template
 * or resource is not found. This allows overriding templates and resources from theme resource providers in the theme.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ThemeResourceProvider extends Provider {

    /**
     * Load the template for the specific name
     *
     * @param name the template name
     * @return the URL of the template, or null if the template is unknown
     * @throws IOException
     */
    URL getTemplate(String name) throws IOException;

    /**
     * Load the resource for the specific path
     *
     * @param path the resource path
     * @return an InputStream to read the resource, or null if the resource is unknown
     * @throws IOException
     */
    InputStream getResourceAsStream(String path) throws IOException;

    /**
     * Load the message bundle for the specific name and locale
     * 
     * @param baseBundlename The base name of the bundle, such as "messages" in
     * messages_en.properties.
     * @param locale The locale of the desired message bundle.
     * @return The localized messages from the bundle.
     * @throws IOException If bundle can not be read.
     */
    default Properties getMessages(String baseBundlename, Locale locale) throws IOException{
        return new Properties();
    }

}
