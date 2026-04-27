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
package org.keycloak.dom.xmlsec.w3.xmlenc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for anonymous complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="DataReference" type="{http://www.w3.org/2001/04/xmlenc#}ReferenceType"/>
 *         &lt;element name="KeyReference" type="{http://www.w3.org/2001/04/xmlenc#}ReferenceType"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class ReferenceList {

    public static class References {

        private ReferenceType dataReference;
        private ReferenceType keyReference;

        public References(ReferenceType dataReference, ReferenceType keyReference) {
            this.dataReference = dataReference;
            this.keyReference = keyReference;
        }

        public ReferenceType getDataReference() {
            return dataReference;
        }

        public ReferenceType getKeyReference() {
            return keyReference;
        }
    }

    private List<References> referencesList = new ArrayList<>();

    public void add(References ref) {
        this.referencesList.add(ref);
    }

    public void addAll(List<References> refs) {
        this.referencesList.addAll(refs);
    }

    public void remove(References ref) {
        this.referencesList.remove(ref);
    }

    public List<References> getReferences() {
        return Collections.unmodifiableList(referencesList);
    }
}