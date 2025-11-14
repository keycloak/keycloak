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
package org.keycloak.dom.saml.v2.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.dom.saml.v2.mdattr.EntityAttributes;
import org.keycloak.dom.saml.v2.mdui.UIInfoType;

import org.w3c.dom.Element;

/**
 * <p>
 * Java class for ExtensionsType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ExtensionsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class ExtensionsType {

    protected List<Object> any = new ArrayList<>();

    /**
     * Function is obsoleted with getAny
     * @return
     */
    @Deprecated
    public Element getElement() {
        return (any.isEmpty()) ? null : (Element) any.get(0);
    }

    /**
     * Function is obsoleted with addExtension
     * @return
     */
    @Deprecated
    public void setElement(Element element) {
        any.clear();
        any.add(element);
    }

    /**
     * Add an extension
     *
     * @param extension
     */
    public void addExtension(Object extension) {
        any.add(extension);
    }

    /**
     * Remove an extension
     *
     * @param extension
     */
    public void removeExtension(Object extension) {
        any.remove(extension);
    }

    /**
     * Gets the value of the any property.
     */
    public List<Object> getAny() {
        return Collections.unmodifiableList(this.any);
    }

    public List<Element> getDomElements() {
        List<Element> output = new ArrayList<Element>();

        for (Object o : this.any) {
            if (o instanceof Element) {
                output.add((Element) o);
            }
        }

        return Collections.unmodifiableList(output);
    }

    public EntityAttributes getEntityAttributes() {
        for (Object o : this.any) {
            if (o instanceof EntityAttributes) {
                return (EntityAttributes) o;
            }
        }
        return null;
    }

    public UIInfoType getUIInfo() {
        for (Object o : this.any) {
            if (o instanceof UIInfoType) {
                return (UIInfoType) o;
            }
        }
        return null;
    }

}
