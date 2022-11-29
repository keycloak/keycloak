/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod;

import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.storage.hotRod.client.HotRodClientEntity;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.keycloak.models.ClientModel.SearchableFields.CLIENT_ID;
import static org.keycloak.models.ClientModel.SearchableFields.ID;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class IckleQueryMapModelCriteriaBuilderTest {
    @Test
    public void testSimpleIckleQuery() {
        IckleQueryMapModelCriteriaBuilder<HotRodClientEntity, ClientModel> v = new IckleQueryMapModelCriteriaBuilder<>(HotRodClientEntity.class);
        IckleQueryMapModelCriteriaBuilder<HotRodClientEntity, ClientModel> mcb = v.compare(CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, 3);
        assertThat(mcb.getIckleQuery(), is(equalTo("FROM kc.HotRodClientEntity c WHERE (c.clientId = :clientId0)")));
        assertThat(mcb.getParameters().entrySet(), hasSize(1));
        assertThat(mcb.getParameters(), hasEntry("clientId0", 3));


        mcb = v.compare(CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, 4)
                .compare(ID, ModelCriteriaBuilder.Operator.EQ, 5);

        assertThat(mcb.getIckleQuery(), is(equalTo("FROM kc.HotRodClientEntity c WHERE ((c.clientId = :clientId0) AND (c.id = :id0))")));
        assertThat(mcb.getParameters().entrySet(), hasSize(2));
        assertThat(mcb.getParameters(), allOf(hasEntry("clientId0", 4), hasEntry("id0", 5)));
    }


    @Test
    public void testSimpleIckleQueryFlashedFromDefault() {
        DefaultModelCriteria<ClientModel> v = criteria();
        IckleQueryMapModelCriteriaBuilder<HotRodClientEntity, ClientModel> mcb = v.compare(CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, 3).flashToModelCriteriaBuilder(new IckleQueryMapModelCriteriaBuilder<>(HotRodClientEntity.class));
        assertThat(mcb.getIckleQuery(), is(equalTo("FROM kc.HotRodClientEntity c WHERE (c.clientId = :clientId0)")));
        assertThat(mcb.getParameters().entrySet(), hasSize(1));
        assertThat(mcb.getParameters(), hasEntry("clientId0", 3));


        mcb = v.compare(CLIENT_ID, ModelCriteriaBuilder.Operator.EQ, 4)
                .compare(ID, ModelCriteriaBuilder.Operator.EQ, 5).flashToModelCriteriaBuilder(new IckleQueryMapModelCriteriaBuilder<>(HotRodClientEntity.class));

        assertThat(mcb.getIckleQuery(), is(equalTo("FROM kc.HotRodClientEntity c WHERE ((c.clientId = :clientId0) AND (c.id = :id0))")));
        assertThat(mcb.getParameters().entrySet(), hasSize(2));
        assertThat(mcb.getParameters(), allOf(hasEntry("clientId0", 4), hasEntry("id0", 5)));
    }

    @Test
    public void testUser() {
        final DefaultModelCriteria<UserModel> mcb = criteria();
        DefaultModelCriteria<UserModel> criteria = mcb.compare(UserModel.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, "realm1");
        criteria = criteria.compare(UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT, ModelCriteriaBuilder.Operator.NOT_EXISTS);
        criteria = mcb.and(criteria, mcb.or(
                mcb.compare(UserModel.SearchableFields.USERNAME, ModelCriteriaBuilder.Operator.ILIKE, "a"),
                mcb.compare(UserModel.SearchableFields.EMAIL, ModelCriteriaBuilder.Operator.ILIKE, "a"),
                mcb.compare(UserModel.SearchableFields.FIRST_NAME, ModelCriteriaBuilder.Operator.ILIKE, "a"),
                mcb.compare(UserModel.SearchableFields.LAST_NAME, ModelCriteriaBuilder.Operator.ILIKE, "a")
        ));

        IckleQueryMapModelCriteriaBuilder<HotRodUserEntity, UserModel> ickle = criteria.flashToModelCriteriaBuilder(new IckleQueryMapModelCriteriaBuilder<>(HotRodUserEntity.class));

        assertThat(ickle.getIckleQuery(), is(equalTo("FROM kc.HotRodUserEntity c WHERE ((c.realmId = :realmId0) AND (c.serviceAccountClientLink IS NULL OR c.serviceAccountClientLink IS EMPTY) AND ((c.usernameLowercase LIKE :usernameLowercase0) OR (c.email : 'a') OR (c.firstName : 'a') OR (c.lastName : 'a')))")));
        assertThat(ickle.getParameters().entrySet(), hasSize(2));
        assertThat(ickle.getParameters(), allOf(hasEntry("realmId0", "realm1"), hasEntry("usernameLowercase0", "a")));
    }
}