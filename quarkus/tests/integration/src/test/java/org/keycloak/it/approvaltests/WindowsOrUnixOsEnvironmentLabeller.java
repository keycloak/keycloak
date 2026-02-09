/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.approvaltests;

import java.util.Locale;

import org.lambda.functions.Function0;

public class WindowsOrUnixOsEnvironmentLabeller implements Function0<String> {

    private static final String WINDOWS_NAME = "windows";
    private static final String UNIX_NAME = "unix";

    @Override
    public String call()
    {
        String osName = System.getProperty("os.name");

        if(osName.toLowerCase(Locale.ROOT).contains(WINDOWS_NAME)) {
            return WINDOWS_NAME;
        }

        //unix suffices, as basically all other OSses use sh files
        return UNIX_NAME;
    }
}
