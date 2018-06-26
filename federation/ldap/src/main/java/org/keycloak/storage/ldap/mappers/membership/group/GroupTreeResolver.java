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

package org.keycloak.storage.ldap.mappers.membership.group;

import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupTreeResolver {

    private static final Logger logger = Logger.getLogger(GroupTreeResolver.class);


    /**
     * Fully resolves list of group trees to be used in Keycloak. The input is group info (usually from LDAP) where each "Group" object contains
     * just it's name and direct children.
     *
     * The operation also performs validation as rules for LDAP are less strict than for Keycloak (In LDAP, the recursion is possible and multiple parents of single group is also allowed)
     *
     * @param groups
     * @param ignoreMissingGroups
     * @return
     * @throws GroupTreeResolveException
     */
    public List<GroupTreeEntry> resolveGroupTree(List<Group> groups, boolean ignoreMissingGroups) throws GroupTreeResolveException {
        // 1- Get parents of each group
        Map<String, List<String>> parentsTree = getParentsTree(groups, ignoreMissingGroups);

        // 2 - Get rootGroups (groups without parent) and check if there is no group with multiple parents
        List<String> rootGroups = new LinkedList<>();
        for (Map.Entry<String, List<String>> group : parentsTree.entrySet()) {
            int parentCount = group.getValue().size();
            if (parentCount == 0) {
                rootGroups.add(group.getKey());
            } else if (parentCount > 1) {
                throw new GroupTreeResolveException("Group '" + group.getKey() + "' detected to have multiple parents. This is not allowed in Keycloak. Parents are: " + group.getValue());
            }
        }

        // 3 - Just convert to map for easier retrieval
        Map<String, Group> asMap = new TreeMap<>();
        for (Group group : groups) {
            asMap.put(group.getGroupName(), group);
        }

        // 4 - Now we have rootGroups. Let's resolve them
        List<GroupTreeEntry> finalResult = new LinkedList<>();
        Set<String> visitedGroups = new TreeSet<>();
        for (String rootGroupName : rootGroups) {
            List<String> subtree = new LinkedList<>();
            subtree.add(rootGroupName);
            GroupTreeEntry groupTree = resolveGroupTree(rootGroupName, asMap, visitedGroups, subtree);
            finalResult.add(groupTree);
        }


        // 5 - Check recursion
        if (visitedGroups.size() != asMap.size()) {
            // Recursion detected. Try to find where it is
            for (Map.Entry<String, Group> entry : asMap.entrySet()) {
                String groupName = entry.getKey();
                if (!visitedGroups.contains(groupName)) {
                    List<String> subtree = new LinkedList<>();
                    subtree.add(groupName);

                    Set<String> newVisitedGroups = new TreeSet<>();
                    resolveGroupTree(groupName, asMap, newVisitedGroups, subtree);
                    visitedGroups.addAll(newVisitedGroups);
                }
            }

            // Shouldn't happen
            throw new GroupTreeResolveException("Illegal state: Recursion detected, but wasn't able to find it");
        }

        return finalResult;
    }

    private Map<String, List<String>> getParentsTree(List<Group> groups, boolean ignoreMissingGroups) throws GroupTreeResolveException {
        Map<String, List<String>> result = new TreeMap<>();

        for (Group group : groups) {
            result.put(group.getGroupName(), new LinkedList<String>());
        }

        for (Group group : groups) {
            Iterator<String> iterator = group.getChildrenNames().iterator();
            while (iterator.hasNext()) {
                String child = iterator.next();
                List<String> list = result.get(child);
                if (list != null) {
                    list.add(group.getGroupName());
                } else if (ignoreMissingGroups) {
                    // Need to remove the missing group
                    iterator.remove();
                    logger.debug("Group '" + child + "' referenced as member of group '" + group.getGroupName() + "' doesn't exists. Ignoring.");
                } else {
                    throw new GroupTreeResolveException("Group '" + child + "' referenced as member of group '" + group.getGroupName() + "' doesn't exists");
                }
            }
        }
        return result;
    }

    private GroupTreeEntry resolveGroupTree(String groupName, Map<String, Group> asMap, Set<String> visitedGroups, List<String> currentSubtree) throws GroupTreeResolveException {
        if (visitedGroups.contains(groupName)) {
            throw new GroupTreeResolveException("Recursion detected when trying to resolve group '" + groupName + "'. Whole recursion path: " + currentSubtree);
        }

        visitedGroups.add(groupName);

        Group group = asMap.get(groupName);

        List<GroupTreeEntry> children = new LinkedList<>();
        GroupTreeEntry result =  new GroupTreeEntry(group.getGroupName(), children);

        for (String childrenName : group.getChildrenNames()) {
            List<String> subtreeCopy = new LinkedList<>(currentSubtree);
            subtreeCopy.add(childrenName);
            GroupTreeEntry childEntry = resolveGroupTree(childrenName, asMap, visitedGroups, subtreeCopy);
            children.add(childEntry);
        }

        return result;
    }



    // static classes

    public static class GroupTreeResolveException extends Exception {

        public GroupTreeResolveException(String message) {
            super(message);
        }
    }


    public static class Group {

        private final String groupName;
        private final List<String> childrenNames;

        public Group(String groupName, String... childrenNames) {
            this(groupName, Arrays.asList(childrenNames));
        }

        public Group(String groupName, Collection<String> childrenNames) {
            this.groupName = groupName;
            this.childrenNames = new LinkedList<>(childrenNames);
        }

        public String getGroupName() {
            return groupName;
        }

        public List<String> getChildrenNames() {
            return childrenNames;
        }
    }

    public static class GroupTreeEntry {

        private final String groupName;
        private final List<GroupTreeEntry> children;

        public GroupTreeEntry(String groupName, List<GroupTreeEntry> children) {
            this.groupName = groupName;
            this.children = children;
        }

        public String getGroupName() {
            return groupName;
        }

        public List<GroupTreeEntry> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("{ " + groupName + " -> [ ");
            for (GroupTreeEntry child : children) {
                builder.append(child.toString());
            }
            builder.append(" ]}");

            return builder.toString();
        }
    }
}
