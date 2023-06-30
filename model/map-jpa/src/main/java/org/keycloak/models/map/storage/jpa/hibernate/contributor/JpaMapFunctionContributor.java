/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.hibernate.contributor;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

/**
 * A {@link FunctionContributor} to register custom functions.
 */
public class JpaMapFunctionContributor implements FunctionContributor {

    private final Logger LOG = Logger.getLogger(JpaMapFunctionContributor.class);

    @Override
    public void contributeFunctions(FunctionContributions fc) {

        fc.getFunctionRegistry().registerPattern("->>", "?1->>?2", getBasicType(fc, StandardBasicTypes.STRING));
        fc.getFunctionRegistry().registerPattern("->", "?1->?2", getJsonbBasicType(fc));
        fc.getFunctionRegistry().registerPattern("@>", "?1@>?2::jsonb", getBasicType(fc, StandardBasicTypes.BOOLEAN));

        contributeDbSpecificFunctions(fc);
    }

    private BasicType getJsonbBasicType(FunctionContributions fc) {
        return fc.getTypeConfiguration().getBasicTypeRegistry().resolve(JsonbType.class, SqlTypes.JSON);
    }

    private BasicType getBasicType(FunctionContributions fc, BasicTypeReference<?> btr) {
        return fc.getTypeConfiguration().getBasicTypeRegistry().resolve(btr);
    }

    private void contributeDbSpecificFunctions(FunctionContributions fc) {
        Dialect dialect = fc.getDialect();
        if (dialect instanceof PostgreSQLDialect) {
            fc.getFunctionRegistry().registerPattern("kc_hash", "sha256(?1::bytea)", getBasicType(fc, StandardBasicTypes.BINARY));
        } else if (dialect instanceof CockroachDialect) {
            fc.getFunctionRegistry().registerPattern("kc_hash", "sha256(?1)", getBasicType(fc, StandardBasicTypes.STRING));
        } else {
            LOG.warnf("Dialect %s not recognized.", dialect);
        }
    }
}
