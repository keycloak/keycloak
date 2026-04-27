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
package org.keycloak.dom.saml.v1.assertion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.dom.saml.common.CommonConditionsType;

/**
 * <complexType name="ConditionsType"> <choice minOccurs="0" maxOccurs="unbounded"> <element
 * ref="saml:AudienceRestrictionCondition"/> <element ref="saml:DoNotCacheCondition"/> <element ref="saml:Condition"/>
 * </choice>
 * <attribute name="NotBefore" type="dateTime" use="optional"/> <attribute name="NotOnOrAfter" type="dateTime"
 * use="optional"/>
 * </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11ConditionsType extends CommonConditionsType {

    public List<SAML11ConditionAbstractType> conditions = new ArrayList<>();

    public void add(SAML11ConditionAbstractType condition) {
        this.conditions.add(condition);
    }

    public void addAll(List<SAML11ConditionAbstractType> theConditions) {
        this.conditions.addAll(theConditions);
    }

    public boolean remove(SAML11ConditionAbstractType condition) {
        return this.conditions.remove(condition);
    }

    public List<SAML11ConditionAbstractType> get() {
        return Collections.unmodifiableList(conditions);
    }
}