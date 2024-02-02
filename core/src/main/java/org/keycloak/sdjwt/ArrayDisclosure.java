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
package org.keycloak.sdjwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Handles selective disclosure of elements within a top-level array claim,
 * supporting both visible and undisclosed elements.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public class ArrayDisclosure extends AbstractSdJwtClaim {
    private final List<SdJwtArrayElement> elements;
    private JsonNode visibleClaimValue = null;
    private final List<DecoyArrayElement> decoyElements;

    private ArrayDisclosure(SdJwtClaimName claimName, List<SdJwtArrayElement> elements,
            List<DecoyArrayElement> decoyElements) {
        super(claimName);
        this.elements = elements;
        this.decoyElements = decoyElements;
    }

    /**
     * Print the array with visible and invisible elements.
     */
    @Override
    public JsonNode getVisibleClaimValue(String hashAlgo) {
        if (visibleClaimValue != null)
            return visibleClaimValue;

        List<JsonNode> visibleElts = new ArrayList<>();
        elements.stream()
                .filter(Objects::nonNull)
                .forEach(e -> visibleElts.add(e.getVisibleValue(hashAlgo)));

        decoyElements.stream()
                .filter(Objects::nonNull)
                .forEach(e -> {
                    if (e.getIndex() < visibleElts.size())
                        visibleElts.add(e.getIndex(), e.getVisibleValue(hashAlgo));
                    else
                        visibleElts.add(e.getVisibleValue(hashAlgo));
                });

        final ArrayNode n = SdJwtUtils.mapper.createArrayNode();
        visibleElts.forEach(n::add);
        visibleClaimValue = n;
        return visibleClaimValue;
    }

    @Override
    public List<String> getDisclosureStrings() {
        final List<String> disclosureStrings = new ArrayList<>();
        elements.stream()
                .filter(Objects::nonNull)
                .forEach(e -> {
                    String disclosureString = e.getDisclosureString();
                    if (disclosureString != null)
                        disclosureStrings.add(disclosureString);
                });
        return disclosureStrings;
    }

    public static class Builder {
        private SdJwtClaimName claimName;
        private final List<SdJwtArrayElement> elements = new ArrayList<>();
        private final List<DecoyArrayElement> decoyElements = new ArrayList<>();

        public Builder withClaimName(String claimName) {
            this.claimName = new SdJwtClaimName(claimName);
            return this;
        }

        public Builder withVisibleElement(JsonNode elementValue) {
            this.elements.add(new VisibleArrayElement(elementValue));
            return this;
        }

        public Builder withUndisclosedElement(SdJwtSalt salt, JsonNode elementValue) {
            SdJwtSalt sdJwtSalt = salt == null ? new SdJwtSalt(SdJwtUtils.randomSalt()) : salt;
            this.elements.add(UndisclosedArrayElement.builder()
                    .withSalt(sdJwtSalt)
                    .withArrayElement(elementValue)
                    .build());
            return this;
        }

        public void withDecoyElt(Integer position, SdJwtSalt salt) {
            SdJwtSalt sdJwtSalt = salt == null ? new SdJwtSalt(SdJwtUtils.randomSalt()) : salt;
            DecoyArrayElement decoyElement = DecoyArrayElement.builder().withSalt(sdJwtSalt).atIndex(position).build();
            this.decoyElements.add(decoyElement);
        }

        public ArrayDisclosure build() {
            return new ArrayDisclosure(claimName, Collections.unmodifiableList(elements),
                    Collections.unmodifiableList(decoyElements));
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
