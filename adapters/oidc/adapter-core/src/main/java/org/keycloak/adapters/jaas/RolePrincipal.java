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

package org.keycloak.adapters.jaas;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RolePrincipal implements Principal, Serializable {

    private String roleName = null;

    public RolePrincipal(String roleName) {
        this.roleName = roleName;
    }

    public boolean equals (Object p) {
        if (! (p instanceof RolePrincipal))
            return false;
        return getName().equals(((RolePrincipal)p).getName());
    }

    public int hashCode () {
        return getName().hashCode();
    }

    public String getName () {
        return this.roleName;
    }

    public String toString ()
    {
        return getName();
    }

}
