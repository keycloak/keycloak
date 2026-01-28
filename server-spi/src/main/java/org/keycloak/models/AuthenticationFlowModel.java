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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlowModel implements Serializable {

    private String id;
    private String alias;
    private String description;
    private String providerId;
    private boolean topLevel;
    private boolean builtIn;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public static class AuthenticationFlowComparator implements Comparator<AuthenticationFlowModel> {
        public static final AuthenticationFlowModel.AuthenticationFlowComparator SINGLETON =
                new AuthenticationFlowModel.AuthenticationFlowComparator();

        @Override
        public int compare(AuthenticationFlowModel left, AuthenticationFlowModel right) {
            //ensure consistent ordering of authenticationFlows.
            String l = left.getAlias() != null ? left.getAlias() : "\0";
            String r = right.getAlias() != null ? right.getAlias() : "\0";
            return l.compareTo(r);
        }
    }
}
