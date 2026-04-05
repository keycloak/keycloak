/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class ProofSerializationTest {
    @Test
    public void testSerializeProofs() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String proofsStr = " { \"jwt\": [\"ewogICJhbGciOiAiRVMyNTYiLAogICJ0eXAiOiAib3BlbmlkNHZjaS1wcm9vZitqd3QiLAogICJqd2siOiB7CiAgICAia3R5IjogIkVDIiwKICAgICJjcnYiOiAiUC0yNTYiLAogICAgIngiOiAiWEdkNU9GU1pwc080VkRRTUZrR3Z0TDVHU2FXWWE3SzBrNGhxUUdLbFBjWSIsCiAgICAieSI6ICJiSXFDaGhoVDdfdnYtYmhuRmVuREljVzVSUjRKTS1nME5sUi1qZGlHemNFIgogIH0KfQo.ewogICJpc3MiOiAib2lkNHZjaS1jbGllbnQiLAogICJhdWQiOiAiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLAogICJpYXQiOiAxNzE4OTU5MzY5LAogICJub25jZSI6ICJOODAxTEpVam1qQ1FDMUpzTm5lTllXWFpqZHQ2UEZSd01pNkpoTTU1OF9JIgp9Cg.mKKrkRkG1BfOzgsKwcZhop74EHflzHslO_NFOloKPnZ-ms6t0SnsTNDQjM_o4FBQAgtv_fnFEWRgnkNIa34gvQ\"] } ";
        Proofs proofs = objectMapper.readValue(proofsStr, Proofs.class);
        assertNotNull("Proofs should not be null", proofs);
        assertNotNull("JWT proofs should not be null", proofs.getJwt());
        assertEquals("Should have one JWT proof", 1, proofs.getJwt().size());
    }
}
