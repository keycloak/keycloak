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

package org.keycloak.storage.ldap.idm.query.internal;

import org.keycloak.storage.ldap.idm.query.Condition;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class CustomLDAPFilter implements Condition {

    private final String customFilter;

    public CustomLDAPFilter(String customFilter) {
        this.customFilter = customFilter;
    }

    @Override
    public String getParameterName() {
        return null;
    }

    @Override
    public void setParameterName(String parameterName) {
    }

    @Override
    public void updateParameterName(String modelParamName, String ldapParamName) {

    }

    @Override
    public void applyCondition(StringBuilder filter) {
        filter.append(customFilter);
    }

    @Override
    public void setBinary(boolean binary) {
    }

    @Override
    public boolean isBinary() {
        return false;
    }
}
