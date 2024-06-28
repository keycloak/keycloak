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

package org.keycloak.models;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AuthenticatorConfigModel implements Serializable {

    private String id;
    private String alias;
    private Map<String, String> config = new HashMap<>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }



    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public static class AuthenticationConfigComparator implements Comparator<AuthenticatorConfigModel> {
        public static final AuthenticatorConfigModel.AuthenticationConfigComparator SINGLETON =
                new AuthenticatorConfigModel.AuthenticationConfigComparator();

        @Override
        public int compare(AuthenticatorConfigModel left, AuthenticatorConfigModel right) {
            //ensure consistent ordering of authenticationFlows.
            String l = left.getAlias() != null ? left.getAlias() : "\0";
            String r = right.getAlias() != null ? right.getAlias() : "\0";
            return l.compareTo(r);
        }
    }
}
