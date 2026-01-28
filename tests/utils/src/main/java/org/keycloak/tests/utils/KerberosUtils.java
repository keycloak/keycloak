/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.tests.utils;

import org.junit.jupiter.api.Assumptions;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosUtils {

    // TODO Figure out if we need to be able to disable Kerberos support for some tests; if so find a better way
    public static boolean isKerberosSupportExpected() {
        String kerberosSupported = System.getProperty("auth.server.kerberos.supported");
        // Supported by default. It is considered unsupported just if explicitly disabled
        return !"false".equals(kerberosSupported);
    }

    public static void assumeKerberosSupportExpected() {
        Assumptions.assumeTrue(isKerberosSupportExpected(), "Kerberos feature is not expected to be supported by auth server");
    }
}
