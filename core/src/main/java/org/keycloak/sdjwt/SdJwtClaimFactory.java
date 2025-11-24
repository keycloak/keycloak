/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.sdjwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Pascal Knueppel
 * @since 13.11.2025
 */
public class SdJwtClaimFactory {

    public static List<SdJwtClaim> parsePayload(ObjectNode objectNode, DisclosureSpec disclosureSpec) {
        List<SdJwtClaim> claims = new ArrayList<>();
        objectNode.properties().forEach(entry -> {
            claims.add(createClaim(entry.getKey(), entry.getValue(), disclosureSpec));
        });
        return claims;
    }

    private static SdJwtClaim createClaim(String claimName, JsonNode claimValue, DisclosureSpec disclosureSpec) {
        DisclosureSpec.DisclosureData disclosureData = disclosureSpec.getUndisclosedClaim(SdJwtClaimName.of(claimName));

        if (disclosureData != null) {
            return createUndisclosedClaim(claimName, claimValue, disclosureData.getSalt());
        }
        else {
            return createArrayOrVisibleClaim(claimName, claimValue, disclosureSpec);
        }
    }

    private static SdJwtClaim createUndisclosedClaim(String claimName, JsonNode claimValue, SdJwtSalt salt) {
        return UndisclosedClaim.builder()
                               .withClaimName(claimName)
                               .withClaimValue(claimValue)
                               .withSalt(salt)
                               .build();
    }

    private static SdJwtClaim createArrayOrVisibleClaim(String claimName, JsonNode claimValue, DisclosureSpec disclosureSpec) {
        SdJwtClaimName sdJwtClaimName = SdJwtClaimName.of(claimName);
        Map<Integer, DisclosureSpec.DisclosureData> undisclosedArrayElts = //
            disclosureSpec.getUndisclosedArrayElts(sdJwtClaimName);
        Map<Integer, DisclosureSpec.DisclosureData> decoyArrayElts = disclosureSpec.getDecoyArrayElts(sdJwtClaimName);

        if (undisclosedArrayElts != null || decoyArrayElts != null) {
            return createArrayDisclosure(claimName, claimValue, undisclosedArrayElts, decoyArrayElts);
        }
        else {
            return VisibleSdJwtClaim.builder()
                                    .withClaimName(claimName)
                                    .withClaimValue(claimValue)
                                    .build();
        }
    }

    private static SdJwtClaim createArrayDisclosure(String claimName, JsonNode claimValue,
                                                    Map<Integer, DisclosureSpec.DisclosureData> undisclosedArrayElts,
                                                    Map<Integer, DisclosureSpec.DisclosureData> decoyArrayElts) {
        ArrayNode arrayNode = validateArrayNode(claimName, claimValue);
        ArrayDisclosure.Builder arrayDisclosureBuilder = ArrayDisclosure.builder().withClaimName(claimName);

        if (undisclosedArrayElts != null) {
            IntStream.range(0, arrayNode.size())
                     .forEach(i -> processArrayElement(arrayDisclosureBuilder, arrayNode.get(i),
                                                       undisclosedArrayElts.get(i)));
        }

        if (decoyArrayElts != null) {
            decoyArrayElts.forEach((key, value) ->
                                       arrayDisclosureBuilder.withDecoyElt(key, value.getSalt()));
        }

        return arrayDisclosureBuilder.build();
    }

    private static ArrayNode validateArrayNode(String claimName, JsonNode claimValue) {
        return Optional.of(claimValue)
                       .filter(v -> v.getNodeType() == JsonNodeType.ARRAY)
                       .map(v -> (ArrayNode) v)
                       .orElseThrow(
                           () -> new IllegalArgumentException("Expected array for claim with name: " + claimName));
    }

    private static void processArrayElement(ArrayDisclosure.Builder builder, JsonNode elementValue,
                                            DisclosureSpec.DisclosureData disclosureData) {
        if (disclosureData != null) {
            builder.withUndisclosedElement(disclosureData.getSalt(), elementValue);
        }
        else {
            builder.withVisibleElement(elementValue);
        }
    }
}
