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

package org.keycloak.adapters.tomcat;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @version $Revision: 1 $
 */
public abstract class GenericPrincipalFactory implements PrincipalFactory {

    @Override
    public GenericPrincipal createPrincipal(Realm realm, final Principal identity, final Set<String> roleSet) {
        Subject subject = new Subject();
        Set<Principal> principals = subject.getPrincipals();
        principals.add(identity);
        final SimpleGroup[] roleSets = getRoleSets(roleSet);
        for (SimpleGroup group : roleSets) {
            String name = group.getName();
            SimpleGroup subjectGroup = createGroup(name, principals);
            // Copy the group members to the Subject group
            Enumeration<? extends Principal> members = group.members();
            while (members.hasMoreElements()) {
                Principal role =  members.nextElement();
                subjectGroup.addMember(role);
            }
        }
        return createPrincipal(getPrincipal(subject), new ArrayList<>(roleSet));
    }

    protected abstract GenericPrincipal createPrincipal(Principal userPrincipal, List<String> roles);

    /**
     * Get the Principal given the authenticated Subject. Currently the first subject that is not of type {@code Group} is
     * considered or the single subject inside the CallerPrincipal group.
     *
     * @param subject
     * @return the authenticated subject
     */
    protected Principal getPrincipal(Subject subject) {
        Principal principal = null;
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                for (Principal p : principals) {
                    if (!(p instanceof SimpleGroup) && principal == null) {
                        principal = p;
                    }
                }
            }
        }
        return principal;
    }

    protected SimpleGroup createGroup(String name, Set<Principal> principals) {
        SimpleGroup roles = null;
        for (final Object next : principals) {
            if (!(next instanceof SimpleGroup)) continue;
            SimpleGroup grp = (SimpleGroup) next;
            if (grp.getName().equals(name)) {
                roles = grp;
                break;
            }
        }
        // If we did not find a group create one
        if (roles == null) {
            roles = new SimpleGroup(name);
            principals.add(roles);
        }
        return roles;
    }

    protected SimpleGroup[] getRoleSets(Collection<String> roleSet) {
        SimpleGroup roles = new SimpleGroup("Roles");
        SimpleGroup[] roleSets = {roles};
        for (String role : roleSet) {
            roles.addMember(new SimplePrincipal(role));
        }
        return roleSets;
    }

}
