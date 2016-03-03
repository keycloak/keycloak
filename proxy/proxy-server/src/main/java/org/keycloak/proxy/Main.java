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

package org.keycloak.proxy;

import io.undertow.Undertow;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String jsonConfig = "proxy.json";
        if (args.length > 0) jsonConfig = args[0];
        File file = new File(jsonConfig);
        if (!file.exists()) {
            System.err.println("No proxy config argument and could not find default file proxy.json");
            System.exit(1);
            return;
        }
        FileInputStream fis = new FileInputStream(file);
        Undertow proxyServer = ProxyServerBuilder.build(fis);
        proxyServer.start();

    }
}
