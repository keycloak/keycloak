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

package org.keycloak.adapters.wildfly;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.logging.Logger;
import org.jboss.security.NestableGroup;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.SubjectInfo;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.identity.plugins.SimpleRole;
import org.jboss.security.identity.plugins.SimpleRoleGroup;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;
import org.keycloak.adapters.undertow.ServletRequestAuthenticator;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflyRequestAuthenticator extends ServletRequestAuthenticator
{
    protected static Logger log = Logger.getLogger(WildflyRequestAuthenticator.class);

    public WildflyRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort,
                                       SecurityContext securityContext, HttpServerExchange exchange,
                                       AdapterTokenStore tokenStore) {
        super(facade, deployment, sslRedirectPort, securityContext, exchange, tokenStore);
    }

    @Override
    protected void propagateKeycloakContext(KeycloakUndertowAccount account) {
        super.propagateKeycloakContext(account);
        SecurityInfoHelper.propagateSessionInfo(account);
        log.debug("propagate security context to wildfly");
        Subject subject = new Subject();
        Set<Principal> principals = subject.getPrincipals();
        principals.add(account.getPrincipal());
        Group[] roleSets = getRoleSets(account.getRoles());
        for (int g = 0; g < roleSets.length; g++) {
            Group group = roleSets[g];
            String name = group.getName();
            Group subjectGroup = createGroup(name, principals);
            if (subjectGroup instanceof NestableGroup) {
                /* A NestableGroup only allows Groups to be added to it so we
                need to add a SimpleGroup to subjectRoles to contain the roles
                */
                SimpleGroup tmp = new SimpleGroup("Roles");
                subjectGroup.addMember(tmp);
                subjectGroup = tmp;
            }
            // Copy the group members to the Subject group
            Enumeration<? extends Principal> members = group.members();
            while (members.hasMoreElements()) {
                Principal role = (Principal) members.nextElement();
                subjectGroup.addMember(role);
            }
        }
        // add the CallerPrincipal group if none has been added in getRoleSets
        Group callerGroup = new SimpleGroup(SecurityConstants.CALLER_PRINCIPAL_GROUP);
        callerGroup.addMember(account.getPrincipal());
        principals.add(callerGroup);
        org.jboss.security.SecurityContext sc = SecurityContextAssociation.getSecurityContext();
        Principal userPrincipal = getPrincipal(subject);
        sc.getUtil().createSubjectInfo(userPrincipal, account, subject);

        // Roles of subjectInfo are null, because is was constructed by
        // org.jboss.security.identity.extensions.CredentialIdentityFactory
        //   .createIdentity(Principal [=userPrincipal], Object [=account], Role [=null]).
        // Therefore the roles are only contained in the authenticatedSubject (member of subjectInfo)
        // and subsequent logics do only access subjectInfo#roles instead of authenticatedSubject#roles.
        mapGroupMembersOfAuthenticatedSubjectIntoSecurityContext(sc);
    }

    /**
     * Get the Principal given the authenticated Subject. Currently the first subject that is not of type {@code Group} is
     * considered or the single subject inside the CallerPrincipal group.
     *
     * @param subject
     * @return the authenticated subject
     */
    protected Principal getPrincipal(Subject subject) {
        Principal principal = null;
        Principal callerPrincipal = null;
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                for (Principal p : principals) {
                    if (!(p instanceof Group) && principal == null) {
                        principal = p;
                    }
                    if (p instanceof Group) {
                        Group g = Group.class.cast(p);
                        if (g.getName().equals(SecurityConstants.CALLER_PRINCIPAL_GROUP) && callerPrincipal == null) {
                            Enumeration<? extends Principal> e = g.members();
                            if (e.hasMoreElements())
                                callerPrincipal = e.nextElement();
                        }
                    }
                }
            }
        }
        return callerPrincipal == null ? principal : callerPrincipal;
    }

    protected Group createGroup(String name, Set<Principal> principals) {
        Group roles = null;
        Iterator<Principal> iter = principals.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (!(next instanceof Group))
                continue;
            Group grp = (Group) next;
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

    protected Group[] getRoleSets(Collection<String> roleSet) {
        SimpleGroup roles = new SimpleGroup("Roles");
        Group[] roleSets = {roles};
        for (String role : roleSet) {
            roles.addMember(new SimplePrincipal(role));
        }
        return roleSets;
    }

    private static void mapGroupMembersOfAuthenticatedSubjectIntoSecurityContext(org.jboss.security.SecurityContext sc) {
        SubjectInfo subjectInfo = sc.getSubjectInfo();
        if (subjectInfo == null) {
            return;
        }

        Subject authenticatedSubject = subjectInfo.getAuthenticatedSubject();
        if (authenticatedSubject == null) {
            return;
        }

        // Get role group of security context in order to add roles of authenticatedSubject.
        RoleGroup scRoles = sc.getUtil().getRoles();
        if (scRoles == null) {
            scRoles = new SimpleRoleGroup("Roles");
            sc.getUtil().setRoles(scRoles);
        }

        // Get group roles of authenticatedSubject and add each role of the group into security context
        Iterator<Principal> principalItr = authenticatedSubject.getPrincipals().iterator();
        while (principalItr.hasNext()) {
            Principal principal = principalItr.next();
            if (principal instanceof Group) {
                Enumeration<? extends Principal> members = ((Group) principal).members();
                while (members.hasMoreElements()) {
                    Principal role = members.nextElement();
                    scRoles.addRole(new SimpleRole(role.getName()));
                }
            }
        }
    }
}
