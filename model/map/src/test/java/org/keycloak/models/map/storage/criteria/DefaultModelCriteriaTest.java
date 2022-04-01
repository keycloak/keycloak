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
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import static org.hamcrest.Matchers.hasToString;
import static org.keycloak.models.ClientModel.SearchableFields.*;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

/**
 *
 * @author hmlnarik
 */
public class DefaultModelCriteriaTest {

    @Test
    public void testSimpleCompare() {
        DefaultModelCriteria<ClientModel> v = criteria();
        assertThat(v.compare(CLIENT_ID, Operator.EQ, 3), hasToString("clientId EQ [3]"));
        assertThat(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5), hasToString("(clientId EQ [4] && id EQ [5])"));
    }

    @Test
    public void testSimpleCompareAnd() {
        DefaultModelCriteria<ClientModel> v = criteria();
        assertThat(v.and(), hasToString("__TRUE__"));
        assertThat(v.and(v.or()), hasToString("__FALSE__"));

        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 3)), hasToString("clientId EQ [3]"));
        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 3), v.or()), hasToString("__FALSE__"));
        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5)), hasToString("(clientId EQ [4] && id EQ [5])"));
        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 4), v.compare(ID, Operator.EQ, 5)), hasToString("(clientId EQ [4] && id EQ [5])"));
    }

    @Test
    public void testSimpleCompareOr() {
        DefaultModelCriteria<ClientModel> v = criteria();
        assertThat(v.or(), hasToString("__FALSE__"));
        assertThat(v.or(v.and()), hasToString("__TRUE__"));

        assertThat(v.or(v.compare(CLIENT_ID, Operator.EQ, 3)), hasToString("clientId EQ [3]"));
        assertThat(v.or(v.compare(CLIENT_ID, Operator.EQ, 3), v.and()), hasToString("__TRUE__"));
        assertThat(v.or(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5)), hasToString("(clientId EQ [4] && id EQ [5])"));
        assertThat(v.or(v.compare(CLIENT_ID, Operator.EQ, 4), v.compare(ID, Operator.EQ, 5)), hasToString("(clientId EQ [4] || id EQ [5])"));
        assertThat(v.or(v.or(v.compare(CLIENT_ID, Operator.EQ, 4), v.compare(ID, Operator.EQ, 5))), hasToString("(clientId EQ [4] || id EQ [5])"));
        assertThat(v.and(v.or(v.compare(CLIENT_ID, Operator.EQ, 4), v.compare(ID, Operator.EQ, 5))), hasToString("(clientId EQ [4] || id EQ [5])"));
    }

    @Test
    public void testSimpleCompareAndOr() {
        DefaultModelCriteria<ClientModel> v = criteria();
        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(ID, Operator.EQ, 5)
          ),
          v.not(v.not(v.compare(ATTRIBUTE, Operator.EQ, "city", "Ankh-Morpork")))
        ), hasToString("((clientId EQ [4] && id EQ [5]) || attribute EQ [city, Ankh-Morpork])"));

        DefaultModelCriteria<RoleModel> mcb = criteria();
        assertThat(mcb.and(
          mcb.compare(RoleModel.SearchableFields.REALM_ID, Operator.EQ, "realmId"),
          mcb.compare(RoleModel.SearchableFields.CLIENT_ID, Operator.EQ, "clientId"),
          mcb.or(
            mcb.compare(RoleModel.SearchableFields.NAME, Operator.ILIKE, "%search%"),
            mcb.compare(RoleModel.SearchableFields.DESCRIPTION, Operator.ILIKE, "%search%")
          )
        ), hasToString("(realmId EQ [realmId] && clientId EQ [clientId] && (name ILIKE [%search%] || description ILIKE [%search%]))"));

        assertThat(mcb
          .compare(RoleModel.SearchableFields.REALM_ID, Operator.EQ, "realmId")
          .compare(RoleModel.SearchableFields.CLIENT_ID, Operator.EQ, "clientId")
          .or(
            mcb.compare(RoleModel.SearchableFields.NAME, Operator.ILIKE, "%search%"),
            mcb.compare(RoleModel.SearchableFields.DESCRIPTION, Operator.ILIKE, "%search%")
          ),
          hasToString("(realmId EQ [realmId] && clientId EQ [clientId] && (name ILIKE [%search%] || description ILIKE [%search%]))"));
    }

    @Test
    public void testComplexCompareAndOr() {
        DefaultModelCriteria<ClientModel> v = criteria();
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

        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(REALM_ID, Operator.EQ, "aa")
          ),
          v.not(
            v.not(
              v.compare(ID, Operator.EQ, 5)
            ).compare(ENABLED, Operator.EQ, "true")
          )
        ),
          hasToString("((clientId EQ [4] && realmId EQ [aa]) || ! (! id EQ [5] && enabled EQ [true]))")
        );

        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(REALM_ID, Operator.EQ, "aa")
          ),
          v.not(
            v.not(
              v.and(
                v.compare(ID, Operator.EQ, 5)
                 .compare(ENABLED, Operator.EQ, "true")
              )
            )
          )
        ),
          hasToString("((clientId EQ [4] && realmId EQ [aa]) || (id EQ [5] && enabled EQ [true]))")
        );
    }

    @Test
    public void testFlashingToAnotherMCB() {
        DefaultModelCriteria<ClientModel> v = criteria();
        assertThat(v.or(
          v.and(
            v.compare(CLIENT_ID, Operator.EQ, 4),
            v.compare(REALM_ID, Operator.EQ, "aa")
          ),
          v.not(v.compare(ID, Operator.EQ, 5))
        ).flashToModelCriteriaBuilder(criteria()),
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
        ).flashToModelCriteriaBuilder(criteria()),
          hasToString("((clientId EQ [4] && realmId EQ [aa]) || (! id EQ [5] && enabled EQ [true]))")
        );
    }

    @Test
    public void testCloning() {
        DefaultModelCriteria<ClientModel> v = criteria();

        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5))
          .partiallyEvaluate((field, operator, operatorArguments) -> 
            (field == CLIENT_ID && operator == Operator.EQ && Arrays.asList(operatorArguments).contains(4))
            || (field == ID && operator == Operator.EQ && Arrays.asList(operatorArguments).contains(5))
              ? true
              : null
          ),
          hasToString("(__TRUE__ && __TRUE__)"));

        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5))
          .partiallyEvaluate((field, operator, operatorArguments) ->
            (field == CLIENT_ID && operator == Operator.EQ && Arrays.asList(operatorArguments).contains(4))
            || (field == ID && operator == Operator.EQ && Arrays.asList(operatorArguments).contains(5))
              ? true
              : null
          ).optimize(),
          hasToString("__TRUE__"));

        assertThat(v.and(v.compare(CLIENT_ID, Operator.EQ, 4).compare(ID, Operator.EQ, 5))
          .partiallyEvaluate((field, operator, operatorArguments) -> field == CLIENT_ID && operator == Operator.EQ && Arrays.asList(operatorArguments).contains(6) ? true : null),
          hasToString("(clientId EQ [4] && id EQ [5])"));
    }

}
