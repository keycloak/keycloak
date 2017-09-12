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

package org.keycloak.models.jpa;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:levente.nagy@itesoft.com">NAGY Léventé</a>
 * @version $Revision: 1 $
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaRealmProviderTest {

    private JpaRealmProvider subject;

    private RealmModel realmModelMock;
    private RealmProvider realmProviderMock;
    private KeycloakSession sessionMock;
    private EntityManager entityManagerMock;

    @Before
    public void setup() {
        realmModelMock = mock(RealmModel.class);
        realmProviderMock = mock(RealmProvider.class);
        sessionMock = mock(KeycloakSession.class);
        entityManagerMock = mock(EntityManager.class);

        subject = new JpaRealmProvider(sessionMock, entityManagerMock);

        // Common behaviours
        when(realmProviderMock.getGroupById(anyString(), any(RealmModel.class))).thenAnswer((Answer<GroupModel>) invocationOnMock -> {
            GroupEntity entity = new GroupEntity();
            entity.setId((String) invocationOnMock.getArguments()[0]);
            entity.setName((String) invocationOnMock.getArguments()[0]);
            return new GroupAdapter(realmModelMock, entityManagerMock, entity);
        });
    }

    @Test
    public void testGetGroupsCountAllGroups() {
        // Given
        Long result = 10L;
        String idRealm = "idGroup";
        TypedQuery<Long> query = mock(TypedQuery.class);

        // When
        when(entityManagerMock.createNamedQuery("getGroupCount", Long.class)).thenReturn(query);
        when(realmModelMock.getId()).thenReturn(idRealm);
        when(query.setParameter("realm", idRealm)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(result);

        // Then
        Long countResult = subject.getGroupsCount(realmModelMock, false);

        assertEquals(result, countResult);
    }

    @Test
    public void testGetGroupsCountOnlyTopLevelGroups() {
        // Given
        Long result = 10L;
        String idRealm = "idGroup";
        TypedQuery<Long> query = mock(TypedQuery.class);

        // When
        when(entityManagerMock.createNamedQuery("getTopLevelGroupCount", Long.class)).thenReturn(query);
        when(realmModelMock.getId()).thenReturn(idRealm);
        when(query.setParameter("realm", idRealm)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(result);

        // Then
        Long countResult = subject.getGroupsCount(realmModelMock, true);

        assertEquals(result, countResult);
    }

    @Test
    public void testSearchForGroupByNameWithAllParams() {
        // Given
        List<String> result = Arrays.asList("idGroup1", "idGroup2", "idGroup3");
        String idRealm = "idGroup";
        TypedQuery<String> query = mock(TypedQuery.class);
        String search = "findMe";
        Integer first = 0;
        Integer max = 10;

        // When
        when(entityManagerMock.createNamedQuery("getGroupIdsByNameContaining", String.class)).thenReturn(query);
        when(realmModelMock.getId()).thenReturn(idRealm);
        when(query.setParameter("realm", idRealm)).thenReturn(query);
        when(query.setParameter("search", search)).thenReturn(query);
        when(query.setFirstResult(first)).thenReturn(query);
        when(query.setMaxResults(max)).thenReturn(query);
        when(query.getResultList()).thenReturn(result);
        when(sessionMock.realms()).thenReturn(realmProviderMock);

        // Then
        List<GroupModel> searchResult = subject.searchForGroupByName(realmModelMock, search, first, max);

        assertEquals(result.size(), searchResult.size());
    }

    @Test
    public void testSearchForGroupByNameWithNullQueryResult() {
        // Given
        String idRealm = "idGroup";
        TypedQuery<String> query = mock(TypedQuery.class);
        String search = "findMe";

        // When
        when(entityManagerMock.createNamedQuery("getGroupIdsByNameContaining", String.class)).thenReturn(query);
        when(realmModelMock.getId()).thenReturn(idRealm);
        when(query.setParameter("realm", idRealm)).thenReturn(query);
        when(query.setParameter("search", search)).thenReturn(query);
        when(query.getResultList()).thenReturn(null);
        when(sessionMock.realms()).thenReturn(realmProviderMock);

        // Then
        List<GroupModel> searchResult = subject.searchForGroupByName(realmModelMock, search, null, null);

        assertEquals(Collections.EMPTY_LIST, searchResult);
    }

    @Test
    public void testSearchForGroupByNameWithNonTopLevelGroupInQueryResult() {
        // Given
        List<String> result = Arrays.asList("idGroup1", "idGroup2", "idGroup3", "idGroup4");
        String idRealm = "idGroup";
        TypedQuery<String> query = mock(TypedQuery.class);
        String search = "findMe";
        Integer first = 0;
        Integer max = 10;

        // When
        when(entityManagerMock.createNamedQuery("getGroupIdsByNameContaining", String.class)).thenReturn(query);
        when(realmModelMock.getId()).thenReturn(idRealm);
        when(query.setParameter("realm", idRealm)).thenReturn(query);
        when(query.setParameter("search", search)).thenReturn(query);
        when(query.setFirstResult(first)).thenReturn(query);
        when(query.setMaxResults(max)).thenReturn(query);
        when(query.getResultList()).thenReturn(result);
        when(sessionMock.realms()).thenReturn(realmProviderMock);
        when(realmProviderMock.getGroupById(anyString(), any(RealmModel.class))).thenAnswer((Answer<GroupModel>) invocationOnMock -> {
            GroupEntity entity = new GroupEntity();
            entity.setId((String) invocationOnMock.getArguments()[0]);
            entity.setName((String) invocationOnMock.getArguments()[0]);
            if(Arrays.asList("idGroup2", "idGroup4").contains(invocationOnMock.getArguments()[0])) {
                entity.setParent(new GroupEntity());
                entity.getParent().setId("idGroup5");
                entity.getParent().setName("idGroup5");
            }
            return new GroupAdapter(realmModelMock, entityManagerMock, entity);
        });

        // Then
        List<GroupModel> searchResult = subject.searchForGroupByName(realmModelMock, search, first, max);

        assertEquals(3,searchResult.size());
    }

    @Test
    public void testGetGroupsCountByNameContaining() {
        // Given
        List<String> result = Arrays.asList("idGroup1", "idGroup2", "idGroup3", "idGroup4");
        String idRealm = "idGroup";
        TypedQuery<String> query = mock(TypedQuery.class);
        String search = "findMe";

        // When
        when(entityManagerMock.createNamedQuery("getGroupIdsByNameContaining", String.class)).thenReturn(query);
        when(realmModelMock.getId()).thenReturn(idRealm);
        when(query.setParameter("realm", idRealm)).thenReturn(query);
        when(query.setParameter("search", search)).thenReturn(query);
        when(query.getResultList()).thenReturn(result);
        when(sessionMock.realms()).thenReturn(realmProviderMock);

        // Then
        Long countResult = subject.getGroupsCountByNameContaining(realmModelMock, search);

        verify(query, never()).setFirstResult(anyInt());
        verify(query, never()).setFirstResult(anyInt());
        assertEquals(result.size(), countResult.intValue());
    }
}
