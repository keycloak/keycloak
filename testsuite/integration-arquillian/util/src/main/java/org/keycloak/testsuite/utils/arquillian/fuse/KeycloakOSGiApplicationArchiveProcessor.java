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

package org.keycloak.testsuite.utils.arquillian.fuse;

import org.jboss.arquillian.container.osgi.OSGiApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakOSGiApplicationArchiveProcessor extends OSGiApplicationArchiveProcessor {

    private static final Logger log = Logger.getLogger(KeycloakOSGiApplicationArchiveProcessor.class);

    // We want to ignore OSGI for exampleAdapter tests
    @Override
    public void process(Archive<?> appArchive, TestClass testClass) {
        Class<?> clazz = testClass.getJavaClass();
        boolean isExampleAdapterTest = isExampleAdapterTest(clazz);

        if (isExampleAdapterTest) {
            log.infof("Ignore OSGiApplicationArchiveProcessor for test %s", clazz.getName());
        } else {
            super.process(appArchive, testClass);
        }
    }

    public static boolean isExampleAdapterTest(Class<?> clazz) {
        Class<?> parent = clazz;
        while (true) {
            parent = parent.getSuperclass();
            if (parent == null) {
                return false;
            } else if (parent.getName().equals("org.keycloak.testsuite.adapter.AbstractExampleAdapterTest")) {
                return true;
            }
        }
    }
}
