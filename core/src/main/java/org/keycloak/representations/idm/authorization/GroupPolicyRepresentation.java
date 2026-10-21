/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.idm.authorization;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPolicyRepresentation extends AbstractPolicyRepresentation {

    private String groupsClaim;
    private Set<GroupDefinition> groups;

    @Override
    public String getType() {
        return "group";
    }

    public String getGroupsClaim() {
        return groupsClaim;
    }

    public void setGroupsClaim(String groupsClaim) {
        this.groupsClaim = groupsClaim;
    }

    public Set<GroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(Set<GroupDefinition> groups) {
        this.groups = groups;
    }

    public void addGroup(String... ids) {
        for (String id : ids) {
            addGroup(id, false);
        }
    }

    public void addGroup(String id, boolean extendChildren) {
        if (groups == null) {
            groups = new HashSet<>();
        }
        groups.add(new GroupDefinition(id, extendChildren));
    }

    public void addGroupPath(String... paths) {
        for (String path : paths) {
            addGroupPath(path, false);
        }
    }

    public void addGroupPath(String path, boolean extendChildren) {
        if (groups == null) {
            groups = new HashSet<>();
        }
        groups.add(new GroupDefinition(null, path, extendChildren));
    }

    public void removeGroup(String... ids) {
        if (groups != null) {
            for (String id : ids) {
                Iterator<GroupDefinition> iterator = groups.iterator();
                while (iterator.hasNext()) {
                    GroupDefinition group = iterator.next();
                    if (id.equals(group.getId()) || (group.getPath() != null && group.getPath().equals(id))) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public static class GroupDefinition implements Comparable<GroupDefinition> {

        private String id;
        private String path;
        private boolean extendChildren;

        public GroupDefinition() {
            this(null);
        }

        public GroupDefinition(String id) {
            this(id, false);
        }

        public GroupDefinition(String id, boolean extendChildren) {
            this(id, null, extendChildren);
        }

        public GroupDefinition(String id, String path, boolean extendChildren) {
            this.id = id;
            this.path = path;
            this.extendChildren = extendChildren;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isExtendChildren() {
            return extendChildren;
        }

        public void setExtendChildren(boolean extendChildren) {
            this.extendChildren = extendChildren;
        }

        @Override
        public int compareTo(GroupDefinition o) {
            if (o.id == null || id == null) {
                return 1;
            }
            return id.compareTo(o.id);
        }
    }
}
