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
package org.keycloak.services.resources.admin;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.GroupPermissionEvaluator;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:levente.nagy@itesoft.com">NAGY Léventé</a>
 * @version $Revision: 1 $
 */
public class GroupsResourceTest {

    private GroupsResource subject;

    private RealmModel realmMock;
    private KeycloakSession sessionMock;
    private AdminPermissionEvaluator authMock;
    private AdminEventBuilder adminEventBuilderMock;

    @Before
    public void setup() {
        realmMock = mock(RealmModel.class);
        sessionMock = mock(KeycloakSession.class);
        authMock = mock(AdminPermissionEvaluator.class);
        when(authMock.groups()).thenReturn(mock(GroupPermissionEvaluator.class));

        adminEventBuilderMock = mock(AdminEventBuilder.class);
        when(adminEventBuilderMock.resource(ResourceType.GROUP)).thenReturn(adminEventBuilderMock);

        subject= new GroupsResource(realmMock, sessionMock, authMock, adminEventBuilderMock);
    }

    @Test
    public void testGetGroupWithAllParams() {
        // Given
        String search = "hello";
        Integer first = 0;
        Integer max = 20;
        String groupId = "groupId";
        String groupName = "groupName";
        GroupModel groupMock = mock(GroupModel.class);
        List<GroupModel> groupsList = Collections.singletonList(groupMock);

        // When
        when(realmMock.searchForGroupByName(search, first, max)).thenReturn(groupsList);
        when(groupMock.getSubGroups()).thenReturn(Collections.EMPTY_SET);
        when(groupMock.getId()).thenReturn(groupId);
        when(groupMock.getName()).thenReturn(groupName);
        when(groupMock.getParent()).thenReturn(null);

        //Then
        List<GroupRepresentation> result = subject.getGroups(search, first,max);

        Assert.assertEquals(groupsList.size(), result.size());
        Assert.assertEquals(groupId, result.get(0).getId());
        Assert.assertEquals(groupName, result.get(0).getName());
        Assert.assertTrue(result.get(0).getSubGroups().isEmpty());
    }

    @Test
    public void testGetGroupWithoutSearch() {
        // Given
        Integer first = 0;
        Integer max = 20;
        String groupId = "groupId";
        String groupName = "groupName";
        GroupModel groupMock = mock(GroupModel.class);
        List<GroupModel> groupsList = Collections.singletonList(groupMock);

        // When
        when(realmMock.getTopLevelGroups(first, max)).thenReturn(groupsList);
        when(groupMock.getSubGroups()).thenReturn(Collections.EMPTY_SET);
        when(groupMock.getId()).thenReturn(groupId);
        when(groupMock.getName()).thenReturn(groupName);
        when(groupMock.getParent()).thenReturn(null);

        //Then
        List<GroupRepresentation> result = subject.getGroups(null, first,max);

        Assert.assertEquals(groupsList.size(), result.size());
        Assert.assertEquals(groupId, result.get(0).getId());
        Assert.assertEquals(groupName, result.get(0).getName());
        Assert.assertTrue(result.get(0).getSubGroups().isEmpty());
    }

    @Test
    public void testGetGroupWithoutSearchAndPagination() {
        // Given
        String groupId = "groupId";
        String groupName = "groupName";
        GroupModel groupMock = mock(GroupModel.class);
        List<GroupModel> groupsList = Collections.singletonList(groupMock);

        // When
        when(realmMock.getTopLevelGroups()).thenReturn(groupsList);
        when(groupMock.getSubGroups()).thenReturn(Collections.EMPTY_SET);
        when(groupMock.getId()).thenReturn(groupId);
        when(groupMock.getName()).thenReturn(groupName);
        when(groupMock.getParent()).thenReturn(null);

        //Then
        List<GroupRepresentation> result = subject.getGroups(null, null, null);

        Assert.assertEquals(groupsList.size(), result.size());
        Assert.assertEquals(groupId, result.get(0).getId());
        Assert.assertEquals(groupName, result.get(0).getName());
        Assert.assertTrue(result.get(0).getSubGroups().isEmpty());
    }

    @Test
    public void testGetGroupCountWithSearchAndTopLevelFlagTrue() {
        // Given
        String search = "search";
        Long countResult = 5L;
        JSONObject response = new JSONObject();
        try {
            response.put("count", countResult);
        } catch (JSONException e) {
            fail(e.getMessage());
        }

        // When
        when(realmMock.getGroupsCountByNameContaining(search)).thenReturn(countResult);

        //Then
        Response restResponse = subject.getGroupCount(search, "true");

        assertEquals(response.toString(), restResponse.getEntity());
        assertEquals(MediaType.APPLICATION_JSON, restResponse.getMediaType().toString());
    }

    @Test
    public void testGetGroupCountWithoutSearchAndTopLevelFlagTrue() {
        // Given
        Long countResult = 5L;
        JSONObject response = new JSONObject();
        try {
            response.put("count", countResult);
        } catch (JSONException e) {
            fail(e.getMessage());
        }

        // When
        when(realmMock.getGroupsCount(true)).thenReturn(countResult);

        //Then
        Response restResponse = subject.getGroupCount(null, "true");

        assertEquals(response.toString(), restResponse.getEntity());
        assertEquals(MediaType.APPLICATION_JSON, restResponse.getMediaType().toString());
    }

    @Test
    public void testGetGroupCountWithoutSearchAndTopLevelFlagFalse() {
        // Given
        Long countResult = 5L;
        JSONObject response = new JSONObject();
        try {
            response.put("count", countResult);
        } catch (JSONException e) {
            fail(e.getMessage());
        }

        // When
        when(realmMock.getGroupsCount(false)).thenReturn(countResult);

        //Then
        Response restResponse = subject.getGroupCount(null, "false");

        assertEquals(response.toString(), restResponse.getEntity());
        assertEquals(MediaType.APPLICATION_JSON, restResponse.getMediaType().toString());
    }

    @Test
    public void testGetGroupCountWithoutSearchAndTopLevelFlagNull() {
        // Given
        Long countResult = 5L;
        JSONObject response = new JSONObject();
        try {
            response.put("count", countResult);
        } catch (JSONException e) {
            fail(e.getMessage());
        }

        // When
        when(realmMock.getGroupsCount(false)).thenReturn(countResult);

        //Then
        Response restResponse = subject.getGroupCount(null, null);

        assertEquals(response.toString(), restResponse.getEntity());
        assertEquals(MediaType.APPLICATION_JSON, restResponse.getMediaType().toString());
    }
}
