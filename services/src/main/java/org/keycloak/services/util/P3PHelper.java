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

package org.keycloak.services.util;

import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.common.util.Resteasy;

/**
 * IE requires P3P header to allow loading cookies from iframes when domain differs from main page (see KEYCLOAK-2828 for more details)
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class P3PHelper {

    public static void addP3PHeader() {
        HttpResponse response = Resteasy.getContextData(HttpResponse.class);
        response.getOutputHeaders().putSingle("P3P", "CP=\"This is not a P3P policy!\"");
    }

}
