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

package org.keycloak.testsuite.utils.undertow;



import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UndertowWarClassLoader extends ClassLoader {

    private final Archive<?> archive;

    public UndertowWarClassLoader(ClassLoader parent, Archive<?> archive) {
        super(parent);
        this.archive = archive;
    }

    @Override
    protected Class<?> findClass(String name) {
        try (InputStream resourceAsStream = getResourceAsStream(name.replace('.', '/') + ".class")) {
            byte[] bytes = IOUtils.toByteArray(resourceAsStream);
            return defineClass(name, bytes, 0, bytes.length);            
        } catch (IOException e) {
            throw new RuntimeException("Failed to find class [" + name + "]", e);
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is == null) {
            String resourcePath = "/WEB-INF/classes";
            if (!name.startsWith("/")) {
                resourcePath = resourcePath + "/";
            }
            resourcePath = resourcePath + name;

            Node node = archive.get(resourcePath);
            if (node == null) {
                return null;
            } else {
                return node.getAsset().openStream();
            }
        } else {
            return is;
        }
    }

}
