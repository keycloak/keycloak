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
public abstract class NamedParameterCondition implements Condition {

    private String parameterName;
    private boolean binary;

    public NamedParameterCondition(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public String getParameterName() {
        return parameterName;
    }

    @Override
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }


    @Override
    public void updateParameterName(String modelParamName, String ldapParamName) {
        if (parameterName.equalsIgnoreCase(modelParamName)) {
            this.parameterName = ldapParamName;
        }
    }

    @Override
    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    @Override
    public boolean isBinary() {
        return binary;
    }
}
