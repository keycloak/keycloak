/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.keycloak.proxy;

import java.util.Set;

/**
 * Representation of a single security constrain matched for a single request.
 *
 * When performing any authentication/authorization check every constraint MUST be satisfied for the request to be allowed to
 * proceed.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SingleConstraintMatch {

    private final SecurityInfo.EmptyRoleSemantic emptyRoleSemantic;
    private final Set<String> requiredRoles;

    public SingleConstraintMatch(SecurityInfo.EmptyRoleSemantic emptyRoleSemantic, Set<String> requiredRoles) {
        this.emptyRoleSemantic = emptyRoleSemantic;
        this.requiredRoles = requiredRoles;
    }

    public SecurityInfo.EmptyRoleSemantic getEmptyRoleSemantic() {
        return emptyRoleSemantic;
    }

    public Set<String> getRequiredRoles() {
        return requiredRoles;
    }

}
