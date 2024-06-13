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
import java.io.Reader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.PropertyResourceBundle;

/**
 * @author <a href="mailto:wadahiro@gmail.com">Hiroyuki Wada</a>
 */
public class PropertiesUtil {

    /**
     * Read a properties file either UTF-8 or if that doesn't work in ISO-8895-1 format.
     * This utilizes the functionality present in JDK 9 to automatically detect the encoding of the resource.
     * A user can specify the standard Java system property <code>java.util.PropertyResourceBundle.encoding</code>
     * to change this.
     * <p />
     * Unfortunately the standard {@link Properties#load(Reader)} doesn't support this automatic decoding,
     * as it is only been implemented for resource files.
     *
     * @see PropertyResourceBundle
     */
    public static void readCharsetAware(Properties properties, InputStream stream) throws IOException {
        PropertyResourceBundle propertyResourceBundle = new PropertyResourceBundle(stream);
        Enumeration<String> keys = propertyResourceBundle.getKeys();
        while(keys.hasMoreElements()) {
            String s = keys.nextElement();
            properties.put(s, propertyResourceBundle.getString(s));
        }
    }

}
