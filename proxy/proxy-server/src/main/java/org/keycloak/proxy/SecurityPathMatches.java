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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class SecurityPathMatches {

    private final boolean denyUncoveredHttpMethods;
    private final PathSecurityInformation defaultPathSecurityInformation;
    private final Map<String, PathSecurityInformation> exactPathRoleInformation;
    private final Map<String, PathSecurityInformation> prefixPathRoleInformation;
    private final Map<String, PathSecurityInformation> extensionRoleInformation;

    private SecurityPathMatches(final boolean denyUncoveredHttpMethods, final PathSecurityInformation defaultPathSecurityInformation, final Map<String, PathSecurityInformation> exactPathRoleInformation, final Map<String, PathSecurityInformation> prefixPathRoleInformation, final Map<String, PathSecurityInformation> extensionRoleInformation) {
        this.denyUncoveredHttpMethods = denyUncoveredHttpMethods;
        this.defaultPathSecurityInformation = defaultPathSecurityInformation;
        this.exactPathRoleInformation = exactPathRoleInformation;
        this.prefixPathRoleInformation = prefixPathRoleInformation;
        this.extensionRoleInformation = extensionRoleInformation;
    }

    /**
     *
     * @return <code>true</code> If no security path information has been defined
     */
    public boolean isEmpty() {
        return defaultPathSecurityInformation.excludedMethodRoles.isEmpty() &&
                defaultPathSecurityInformation.perMethodRequiredRoles.isEmpty() &&
                defaultPathSecurityInformation.defaultRequiredRoles.isEmpty() &&
                exactPathRoleInformation.isEmpty() &&
                prefixPathRoleInformation.isEmpty() &&
                extensionRoleInformation.isEmpty();
    }

    public SingleConstraintMatch getSecurityInfo(final String path, final String method) {
        RuntimeMatch currentMatch = new RuntimeMatch();
        handleMatch(method, defaultPathSecurityInformation, currentMatch);
        PathSecurityInformation match = exactPathRoleInformation.get(path);
        if (match != null) {
            handleMatch(method, match, currentMatch);
            return mergeConstraints(currentMatch);
        }

        match = prefixPathRoleInformation.get(path);
        if (match != null) {
            handleMatch(method, match, currentMatch);
            return mergeConstraints(currentMatch);
        }

        int qsPos = -1;
        boolean extension = false;
        for (int i = path.length() - 1; i >= 0; --i) {
            final char c = path.charAt(i);
            if (c == '?') {
                //there was a query string, check the exact matches again
                final String part = path.substring(0, i);
                match = exactPathRoleInformation.get(part);
                if (match != null) {
                    handleMatch(method, match, currentMatch);
                    return mergeConstraints(currentMatch);
                }
                qsPos = i;
                extension = false;
            } else if (c == '/') {
                extension = true;
                final String part = path.substring(0, i);
                match = prefixPathRoleInformation.get(part);
                if (match != null) {
                    handleMatch(method, match, currentMatch);
                    return mergeConstraints(currentMatch);
                }
            } else if (c == '.') {
                if (!extension) {
                    extension = true;
                    final String ext;
                    if (qsPos == -1) {
                        ext = path.substring(i + 1, path.length());
                    } else {
                        ext = path.substring(i + 1, qsPos);
                    }
                    match = extensionRoleInformation.get(ext);
                    if (match != null) {
                        handleMatch(method, match, currentMatch);
                        return mergeConstraints(currentMatch);
                    }
                }
            }
        }
        return mergeConstraints(currentMatch);
    }

    /**
     * merge all constraints, as per 13.8.1 Combining Constraints
     */
    private SingleConstraintMatch mergeConstraints(final RuntimeMatch currentMatch) {
        if(currentMatch.uncovered && denyUncoveredHttpMethods) {
            return new SingleConstraintMatch(SecurityInfo.EmptyRoleSemantic.DENY, Collections.<String>emptySet());
        }
        final Set<String> allowedRoles = new HashSet<String>();
        for(SingleConstraintMatch match : currentMatch.constraints) {
            if(match.getRequiredRoles().isEmpty()) {
                return new SingleConstraintMatch(match.getEmptyRoleSemantic(), Collections.<String>emptySet());
            } else {
                allowedRoles.addAll(match.getRequiredRoles());
            }
        }
        return new SingleConstraintMatch(SecurityInfo.EmptyRoleSemantic.PERMIT, allowedRoles);
    }

    private void handleMatch(final String method, final PathSecurityInformation exact, RuntimeMatch currentMatch) {
        List<SecurityInformation> roles = exact.defaultRequiredRoles;
        for (SecurityInformation role : roles) {
            currentMatch.constraints.add(new SingleConstraintMatch(role.emptyRoleSemantic, role.roles));
            if(role.emptyRoleSemantic == SecurityInfo.EmptyRoleSemantic.DENY || !role.roles.isEmpty()) {
                currentMatch.uncovered = false;
            }
        }
        List<SecurityInformation> methodInfo = exact.perMethodRequiredRoles.get(method);
        if (methodInfo != null) {
            currentMatch.uncovered = false;
            for (SecurityInformation role : methodInfo) {
                currentMatch.constraints.add(new SingleConstraintMatch(role.emptyRoleSemantic, role.roles));
            }
        }
        for (ExcludedMethodRoles excluded : exact.excludedMethodRoles) {
            if (!excluded.methods.contains(method)) {
                currentMatch.uncovered = false;
                currentMatch.constraints.add(new SingleConstraintMatch(excluded.securityInformation.emptyRoleSemantic, excluded.securityInformation.roles));
            }
        }
    }

     public static class Builder {
        private final PathSecurityInformation defaultPathSecurityInformation = new PathSecurityInformation();
        private final Map<String, PathSecurityInformation> exactPathRoleInformation = new HashMap<String, PathSecurityInformation>();
        private final Map<String, PathSecurityInformation> prefixPathRoleInformation = new HashMap<String, PathSecurityInformation>();
        private final Map<String, PathSecurityInformation> extensionRoleInformation = new HashMap<String, PathSecurityInformation>();

        public void addSecurityConstraint(Set<String> roles, SecurityInfo.EmptyRoleSemantic emptyRoleSemantic, String pattern, Set<String> httpMethods, Set<String> excludedMethods) {
            final SecurityInformation securityInformation = new SecurityInformation(roles, emptyRoleSemantic);
            if (pattern.endsWith("/*") || pattern.endsWith("/")) {
                String part = pattern.substring(0, pattern.lastIndexOf('/'));
                PathSecurityInformation info = prefixPathRoleInformation.get(part);
                if (info == null) {
                    prefixPathRoleInformation.put(part, info = new PathSecurityInformation());
                }
                setupPathSecurityInformation(info, securityInformation, httpMethods, excludedMethods);
            } else if (pattern.startsWith("*.")) {
                String part = pattern.substring(2, pattern.length());
                PathSecurityInformation info = extensionRoleInformation.get(part);
                if (info == null) {
                    extensionRoleInformation.put(part, info = new PathSecurityInformation());
                }
                setupPathSecurityInformation(info, securityInformation, httpMethods, excludedMethods);
            } else {
                PathSecurityInformation info = exactPathRoleInformation.get(pattern);
                if (info == null) {
                    exactPathRoleInformation.put(pattern, info = new PathSecurityInformation());
                }
                setupPathSecurityInformation(info, securityInformation, httpMethods, excludedMethods);
            }

        }

        private Set<String> expandRolesAllowed(final Set<String> rolesAllowed) {
            final Set<String> roles = new HashSet<String>(rolesAllowed);
            return roles;
        }

        private void setupPathSecurityInformation(final PathSecurityInformation info, final SecurityInformation securityConstraint,
                                                  Set<String> httpMethods, Set<String> excludedMethods) {
            if (httpMethods.isEmpty() &&
                    excludedMethods.isEmpty()) {
                info.defaultRequiredRoles.add(securityConstraint);
            } else if (!httpMethods.isEmpty()) {
                for (String method : httpMethods) {
                    List<SecurityInformation> securityInformations = info.perMethodRequiredRoles.get(method);
                    if (securityInformations == null) {
                        info.perMethodRequiredRoles.put(method, securityInformations = new ArrayList<SecurityInformation>());
                    }
                    securityInformations.add(securityConstraint);
                }
            } else if (!excludedMethods.isEmpty()) {
                info.excludedMethodRoles.add(new ExcludedMethodRoles(excludedMethods, securityConstraint));
            }
        }

        public SecurityPathMatches build() {
            return new SecurityPathMatches(false, defaultPathSecurityInformation, exactPathRoleInformation, prefixPathRoleInformation, extensionRoleInformation);
        }
    }


    private static class PathSecurityInformation {
        final List<SecurityInformation> defaultRequiredRoles = new ArrayList<SecurityInformation>();
        final Map<String, List<SecurityInformation>> perMethodRequiredRoles = new HashMap<String, List<SecurityInformation>>();
        final List<ExcludedMethodRoles> excludedMethodRoles = new ArrayList<ExcludedMethodRoles>();
    }

    private static final class ExcludedMethodRoles {
        final Set<String> methods;
        final SecurityInformation securityInformation;

        public ExcludedMethodRoles(final Set<String> methods, final SecurityInformation securityInformation) {
            this.methods = methods;
            this.securityInformation = securityInformation;
        }
    }

    private static final class SecurityInformation {
        final Set<String> roles;
        final SecurityInfo.EmptyRoleSemantic emptyRoleSemantic;

        private SecurityInformation(final Set<String> roles, final SecurityInfo.EmptyRoleSemantic emptyRoleSemantic) {
            this.emptyRoleSemantic = emptyRoleSemantic;
            this.roles = new HashSet<String>(roles);
        }
    }

    private static final class RuntimeMatch {
        final List<SingleConstraintMatch> constraints = new ArrayList<SingleConstraintMatch>();
        boolean uncovered = true;
    }
}
