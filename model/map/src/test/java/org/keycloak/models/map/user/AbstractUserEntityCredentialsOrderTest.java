/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.user;

import org.junit.Before;
import org.junit.Test;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;
import org.keycloak.credential.CredentialModel;

import java.util.List;
import java.util.stream.Collectors;

public class AbstractUserEntityCredentialsOrderTest {

    private MapUserEntity<Integer> user;
    
    @Before
    public void init() {
        user = new MapUserEntity<Integer>(1, "realmId") {};
        
        for (int i = 1; i <= 5; i++) {
            UserCredentialEntity credentialModel = new UserCredentialEntity();
            credentialModel.setId(Integer.toString(i));

            user.addCredential(credentialModel);
        }
    }

    private void assertOrder(Integer... ids) {
        List<Integer> currentList = user.getCredentials().map(entity -> Integer.valueOf(entity.getId())).collect(Collectors.toList());
        assertThat(currentList, Matchers.contains(ids));
    }

    @Test
    public void testCorrectOrder() {
        assertOrder(1, 2, 3, 4, 5);
    }

    @Test
    public void testMoveToZero() {
        user.moveCredential(2, 0);
        assertOrder(3, 1, 2, 4, 5);
    }

    @Test
    public void testMoveBack() {
        user.moveCredential(3, 1);
        assertOrder(1, 4, 2, 3, 5);
    }

    @Test
    public void testMoveForward() {
        user.moveCredential(1, 3);
        assertOrder(1, 3, 4, 2, 5);
    }

    @Test
    public void testSamePosition() {
        user.moveCredential(1, 1);
        assertOrder(1, 2, 3, 4, 5);
    }

    @Test
    public void testSamePositionZero() {
        user.moveCredential(0, 0);
        assertOrder(1, 2, 3, 4, 5);
    }

}