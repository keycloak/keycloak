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

package org.keycloak.storage.ldap.idm.query;

/**
 * <p>A {@link Condition} is used to specify how a specific query parameter
 * is defined in order to filter query results.</p>
 *
 * @author Pedro Igor
 */
public interface Condition {

    String getParameterName();
    void setParameterName(String parameterName);

    /**
     * Will change the parameter name if it is "modelParamName" to "ldapParamName" . Implementation can apply this to subconditions as well.
     *
     * It is used to update LDAP queries, which were created with model parameter name ( for example "firstName" ) and rewrite them to use real
     * LDAP mapped attribute (for example "givenName" )
     */
    void updateParameterName(String modelParamName, String ldapParamName);


    void applyCondition(StringBuilder filter);

    void setBinary(boolean binary);

    boolean isBinary();

}