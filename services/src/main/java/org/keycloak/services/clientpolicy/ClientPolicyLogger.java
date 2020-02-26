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

package org.keycloak.services.clientpolicy;

import org.jboss.logging.Logger;

public class ClientPolicyLogger {

    public static void log(Logger logger, String content) {
        if(!logger.isTraceEnabled()) return;
        String buf = new StringBuffer()
            .append("#").append(getMethodName())
            .append(", ").append(content)
            .toString();
        logger.trace(buf);
    }

    public static void logv(Logger logger, String format, Object...params) {
        if(!logger.isTraceEnabled()) return;
        String buf = new StringBuffer()
            .append("#").append(getMethodName())
            .append(", ").append(format)
            .toString();
        logger.tracev(buf, params);
    }

    private static String getClassName() {
        return Thread.currentThread().getStackTrace()[2].getClassName();
    }

    private static String getMethodName() {
        return Thread.currentThread().getStackTrace()[3].getMethodName();
    }
}
