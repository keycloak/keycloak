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
package org.keycloak.models.map.storage.criteria;

import org.keycloak.models.ClientModel;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import static org.hamcrest.Matchers.hasToString;
import static org.keycloak.models.ClientModel.SearchableFields.*;

/**
 *
 * @author hmlnarik
 */
public class DefaultModelCriteriaTest {

    @Test
    public void testSimpleCompare() {
        DefaultModelCriteria<ClientModel> v = new DefaultModelCriteria<>();
        assertThat(v.compare(CLIENT_ID, Operator.EQ, 3), hasToString("clientId EQ [3]"));
        assertThat(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5), hasToString("(clientId EQ [4] && id EQ [5])"));
    }

    @Test
    public void testSimpleCompareAnd() {
        DefaultModelCriteria<ClientModel> v = new DefaultModelCriteria<>();
        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 3)), hasToString("(clientId EQ [3])"));
        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5)), hasToString("((clientId EQ [4] && id EQ [5]))"));
        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 4), v.compare(ID, Operator.EQ, 5)), hasToString("(clientId EQ [4] && id EQ [5])"));
    }

    @Test
    public void testSimpleCompareOr() {
        DefaultModelCriteria<ClientModel> v = new DefaultModelCriteria<>();
        assertThat(v.or(v.compare(CLIENT_ID, Operator.EQ, 3)), hasToString("(clientId EQ [3])"));
        assertThat(v.or(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5)), hasToString("((clientId EQ [4] && id EQ [5]))"));
        assertThat(v.or(v.compare(CLIENT_ID, Operator.EQ, 4), v.compare(ID, Operator.EQ, 5)), hasToString("(clientId EQ [4] || id EQ [5])"));
    }

    @Test
    public void testSimpleCompareAndOr() {
        DefaultModelCriteria<ClientModel> v = new DefaultModelCriteria<>();
        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(ID, Operator.EQ, 5)
          ),
          v.compare(ATTRIBUTE, Operator.EQ, "city", "Ankh-Morpork")
        ), hasToString("((clientId EQ [4] && id EQ [5]) || attribute EQ [city, Ankh-Morpork])"));
    }

    @Test
    public void testComplexCompareAndOr() {
        DefaultModelCriteria<ClientModel> v = new DefaultModelCriteria<>();
        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(REALM_ID, Operator.EQ, "aa")
          ),
          v.not(v.compare(ID, Operator.EQ, 5))
        ),
          hasToString("((clientId EQ [4] && realmId EQ [aa]) || ! id EQ [5])")
        );

        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(REALM_ID, Operator.EQ, "aa")
          ),
          v.not(
            v.compare(ID, Operator.EQ, 5)
          ).compare(ENABLED, Operator.EQ, "true")
        ),
          hasToString("((clientId EQ [4] && realmId EQ [aa]) || (! id EQ [5] && enabled EQ [true]))")
        );
    }

    @Test
    public void testFlashingToAnotherMCB() {
        DefaultModelCriteria<ClientModel> v = new DefaultModelCriteria<>();
        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(REALM_ID, Operator.EQ, "aa")
          ),
          v.not(v.compare(ID, Operator.EQ, 5))
        ).flashToModelCriteriaBuilder(new DefaultModelCriteria<>()),
          hasToString("((clientId EQ [4] && realmId EQ [aa]) || ! id EQ [5])")
        );

        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(REALM_ID, Operator.EQ, "aa")
          ),
          v.not(
            v.compare(ID, Operator.EQ, 5)
          ).compare(ENABLED, Operator.EQ, "true")
        ).flashToModelCriteriaBuilder(new DefaultModelCriteria<>()),
          hasToString("((clientId EQ [4] && realmId EQ [aa]) || (! id EQ [5] && enabled EQ [true]))")
        );
    }

}
