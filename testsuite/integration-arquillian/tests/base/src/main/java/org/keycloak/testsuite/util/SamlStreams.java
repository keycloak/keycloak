/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.ConditionAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.ResponseType.RTChoiceType;
import java.util.Objects;
import java.util.stream.Stream;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class SamlStreams {

    public static Stream<AssertionType> assertionsUnencrypted(SAML2Object ob) {
        assertThat(ob, instanceOf(ResponseType.class));
        return ((ResponseType) ob).getAssertions().stream().map(RTChoiceType::getAssertion).filter(Objects::nonNull);
    }

    public static Stream<AttributeStatementType> attributeStatements(Stream<AssertionType> ob) {
        return ob.flatMap((assertionType) -> assertionType.getAttributeStatements().stream());
    }

    public static Stream<AttributeType> attributesUnecrypted(Stream<AttributeStatementType> ob) {
        return ob.flatMap(ast -> ast.getAttributes().stream()).map(ASTChoiceType::getAttribute).filter(Objects::nonNull);
    }

    public static Stream<ConditionAbstractType> conditions(Stream<AssertionType> ob) {
        return ob.map(AssertionType::getConditions)
          .flatMap(ct -> ct.getConditions().stream());
    }

}
