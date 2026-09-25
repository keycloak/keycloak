/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.workflow.conditions;

import org.keycloak.models.workflow.WorkflowInvalidStateException;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.models.workflow.conditions.UserAttributeWorkflowConditionProvider.parseKeyValuePair;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Configuration parsing shared by the attribute conditions. Pins the properties format the
 * documentation specifies, and pins that input resolving to no entry or to several is
 * rejected rather than silently matching some attribute.
 */
public class UserAttributeWorkflowConditionProviderTest {

    private static void assertParsesTo(String configuration, String expectedKey, String expectedValue) {
        String[] parsed = parseKeyValuePair(configuration);

        assertThat(parsed[0], is(expectedKey));
        assertThat(parsed[1], is(expectedValue));
    }

    private static String rejectionKeyOf(String configuration) {
        return Assert.assertThrows(WorkflowInvalidStateException.class,
                () -> parseKeyValuePair(configuration)).getMessage();
    }

    @Test
    public void parsesBothDocumentedSeparators() {
        // '=' is the separator used throughout the docs, ':' the one named in the admin messages
        assertParsesTo("plan=gold", "plan", "gold");
        assertParsesTo("plan:gold", "plan", "gold");
    }

    @Test
    public void parsesAPresenceOnlyKey() {
        assertParsesTo("plan", "plan", "");
        assertParsesTo("plan:", "plan", "");
    }

    @Test
    public void keepsMultipleValuesAsOneString() {
        // Splitting on ',' belongs to the caller, not to the parser
        assertParsesTo("plan:gold,silver", "plan", "gold,silver");
    }

    @Test
    public void unescapesAnEscapedSeparatorInTheKey() {
        // The escaped-separator handling this method delegates to Properties for
        assertParsesTo("with\\:colon:gold", "with:colon", "gold");
    }

    @Test
    public void rejectsInputWithNoEntry() {
        // Previously fell through to NoSuchElementException from the iterator
        assertThat(rejectionKeyOf(""), is("workflowConditionAttributeInvalid"));
        assertThat(rejectionKeyOf("   "), is("workflowConditionAttributeInvalid"));
        assertThat(rejectionKeyOf("#plan=gold"), is("workflowConditionAttributeInvalid"));
        assertThat(rejectionKeyOf("!plan=gold"), is("workflowConditionAttributeInvalid"));
    }

    @Test
    public void rejectsInputWithMoreThanOneEntry() {
        // Previously resolved to an arbitrary entry, Properties being a Hashtable
        assertThat(rejectionKeyOf("plan=gold\ntier=silver"), is("workflowConditionAttributeInvalid"));
        assertThat(rejectionKeyOf("plan=gold\r\ntier=silver"), is("workflowConditionAttributeInvalid"));
    }

    @Test
    public void toleratesATrailingNewline() {
        assertParsesTo("plan=gold\n", "plan", "gold");
    }
}
