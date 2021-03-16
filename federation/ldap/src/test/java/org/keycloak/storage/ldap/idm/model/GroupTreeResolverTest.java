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

package org.keycloak.storage.ldap.idm.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.storage.ldap.mappers.membership.group.GroupTreeResolver;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupTreeResolverTest {

    @Test
    public void testGroupResolvingCorrect() throws GroupTreeResolver.GroupTreeResolveException {
        GroupTreeResolver.Group group1 = new GroupTreeResolver.Group("group1", "group2", "group3");
        GroupTreeResolver.Group group2 = new GroupTreeResolver.Group("group2", "group4", "group5");
        GroupTreeResolver.Group group3 = new GroupTreeResolver.Group("group3", "group6");
        GroupTreeResolver.Group group4 = new GroupTreeResolver.Group("group4");
        GroupTreeResolver.Group group5 = new GroupTreeResolver.Group("group5");
        GroupTreeResolver.Group group6 = new GroupTreeResolver.Group("group6", "group7");
        GroupTreeResolver.Group group7 = new GroupTreeResolver.Group("group7");
        List<GroupTreeResolver.Group> groups = Arrays.asList(group1, group2, group3, group4, group5, group6, group7);

        GroupTreeResolver resolver = new GroupTreeResolver();
        List<GroupTreeResolver.GroupTreeEntry> groupTree = resolver.resolveGroupTree(groups, false);
        Assert.assertEquals(1, groupTree.size());
        Assert.assertEquals("{ group1 -> [ { group2 -> [ { group4 -> [  ]}{ group5 -> [  ]} ]}{ group3 -> [ { group6 -> [ { group7 -> [  ]} ]} ]} ]}", groupTree.get(0).toString());
    }

    @Test
    public void testGroupResolvingCorrect2_multipleRootGroups() throws GroupTreeResolver.GroupTreeResolveException {
        GroupTreeResolver.Group group1 = new GroupTreeResolver.Group("group1", "group8");
        GroupTreeResolver.Group group2 = new GroupTreeResolver.Group("group2");
        GroupTreeResolver.Group group3 = new GroupTreeResolver.Group("group3", "group2");
        GroupTreeResolver.Group group4 = new GroupTreeResolver.Group("group4", "group1", "group5");
        GroupTreeResolver.Group group5 = new GroupTreeResolver.Group("group5", "group6", "group7");
        GroupTreeResolver.Group group6 = new GroupTreeResolver.Group("group6");
        GroupTreeResolver.Group group7 = new GroupTreeResolver.Group("group7");
        GroupTreeResolver.Group group8 = new GroupTreeResolver.Group("group8", "group9");
        GroupTreeResolver.Group group9 = new GroupTreeResolver.Group("group9");
        List<GroupTreeResolver.Group> groups = Arrays.asList(group1, group2, group3, group4, group5, group6, group7, group8, group9);

        GroupTreeResolver resolver = new GroupTreeResolver();
        List<GroupTreeResolver.GroupTreeEntry> groupTree = resolver.resolveGroupTree(groups, false);

        Assert.assertEquals(2, groupTree.size());
        Assert.assertEquals("{ group3 -> [ { group2 -> [  ]} ]}", groupTree.get(0).toString());
        Assert.assertEquals("{ group4 -> [ { group1 -> [ { group8 -> [ { group9 -> [  ]} ]} ]}{ group5 -> [ { group6 -> [  ]}{ group7 -> [  ]} ]} ]}", groupTree.get(1).toString());
    }


    @Test
    public void testGroupResolvingRecursion() {
        GroupTreeResolver.Group group1 = new GroupTreeResolver.Group("group1", "group2", "group3");
        GroupTreeResolver.Group group2 = new GroupTreeResolver.Group("group2");
        GroupTreeResolver.Group group3 = new GroupTreeResolver.Group("group3", "group4");
        GroupTreeResolver.Group group4 = new GroupTreeResolver.Group("group4", "group5");
        GroupTreeResolver.Group group5 = new GroupTreeResolver.Group("group5", "group1");
        GroupTreeResolver.Group group6 = new GroupTreeResolver.Group("group6", "group7");
        GroupTreeResolver.Group group7 = new GroupTreeResolver.Group("group7");
        List<GroupTreeResolver.Group> groups = Arrays.asList(group1, group2, group3, group4, group5, group6, group7);

        GroupTreeResolver resolver = new GroupTreeResolver();
        try {
            resolver.resolveGroupTree(groups, false);
            Assert.fail("Exception expected because of recursion");
        } catch (GroupTreeResolver.GroupTreeResolveException gre) {
            Assert.assertTrue(gre.getMessage().startsWith("Recursion detected"));
        }
    }

    @Test
    public void testGroupResolvingMultipleParents() {
        GroupTreeResolver.Group group1 = new GroupTreeResolver.Group("group1", "group2");
        GroupTreeResolver.Group group2 = new GroupTreeResolver.Group("group2");
        GroupTreeResolver.Group group3 = new GroupTreeResolver.Group("group3", "group2");
        GroupTreeResolver.Group group4 = new GroupTreeResolver.Group("group4", "group1", "group5");
        GroupTreeResolver.Group group5 = new GroupTreeResolver.Group("group5", "group4");
        List<GroupTreeResolver.Group> groups = Arrays.asList(group1, group2, group3, group4, group5);

        GroupTreeResolver resolver = new GroupTreeResolver();
        try {
            resolver.resolveGroupTree(groups, false);
            Assert.fail("Exception expected because of some groups have multiple parents");
        } catch (GroupTreeResolver.GroupTreeResolveException gre) {
            Assert.assertTrue(gre.getMessage().contains("detected to have multiple parents"));
        }
    }


    @Test
    public void testGroupResolvingMissingGroup() throws GroupTreeResolver.GroupTreeResolveException {
        GroupTreeResolver.Group group1 = new GroupTreeResolver.Group("group1", "group2");
        GroupTreeResolver.Group group2 = new GroupTreeResolver.Group("group2", "group3", "group5");
        GroupTreeResolver.Group group4 = new GroupTreeResolver.Group("group4");
        List<GroupTreeResolver.Group> groups = Arrays.asList(group1, group2, group4);

        GroupTreeResolver resolver = new GroupTreeResolver();
        try {
            resolver.resolveGroupTree(groups, false);
            Assert.fail("Exception expected because of missing referenced group");
        } catch (GroupTreeResolver.GroupTreeResolveException gre) {
            Assert.assertEquals("Group 'group3' referenced as member of group 'group2' doesn't exist", gre.getMessage());
        }

        List<GroupTreeResolver.GroupTreeEntry> groupTree = resolver.resolveGroupTree(groups, true);

        Assert.assertEquals(2, groupTree.size());
        Assert.assertEquals("{ group1 -> [ { group2 -> [  ]} ]}", groupTree.get(0).toString());
        Assert.assertEquals("{ group4 -> [  ]}", groupTree.get(1).toString());
    }
}
