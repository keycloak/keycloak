/*
 * JBoss, Home of Professional Open Source
 * Copyright 2026 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.saml;

import java.util.Arrays;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test BaseSAML2BindingBuilder
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class SAML2BindingBuilderTest {


    /**
     * Test that SAMLRequest or SAMLResponse is not duplicate after redirect URI generation
     *
     * @throws Exception
     */
    @Test
    public void samlReqRespDuplicationTest() throws Exception {

        TestSAML2BindingBuilder saml2BindingBuilder = new TestSAML2BindingBuilder();
        saml2BindingBuilder.relayState("RS1");
        TestSAML2BindingBuilder.RedirectBindingBuilder builder = new TestSAML2BindingBuilder.RedirectBindingBuilder(saml2BindingBuilder);

        // SAMLRequest test
        checkRedirectUriOperation(builder, generateDoubledSamlParams(true, false), true, false);
        // SAMLResponse test
        checkRedirectUriOperation(builder, generateDoubledSamlParams(false, true), false, true);
    }

    /**
     * Test that RelayState is not duplicate after redirect URI generation
     *
     * @throws Exception
     */
    @Test
    public void relayStateDuplicationTest() throws Exception {

        TestSAML2BindingBuilder saml2BindingBuilder = new TestSAML2BindingBuilder();
        saml2BindingBuilder.relayState("RS1");
        TestSAML2BindingBuilder.RedirectBindingBuilder builder = new TestSAML2BindingBuilder.RedirectBindingBuilder(saml2BindingBuilder);

        // SAMLRequest test
        checkRedirectUriOperation(builder, generategenerateDoubledSamlParamsWithRelayState(true, true), true, true);
        // SAMLResponse test
        checkRedirectUriOperation(builder, generategenerateDoubledSamlParamsWithRelayState(false, false), false, false);
    }



    private String faultyEncode(String value) {
        // unnecessary encode which might render compare of paramName useless
        return value.replace("S", "%53").replace("A", "%41");
    }

    private String[] generateDoubledSamlParams(boolean asRequest, boolean faultyEncode) {
        String[] samlParams;
        if (faultyEncode) {
            samlParams = new String[] {
                    asRequest ? faultyEncode(GeneralConstants.SAML_REQUEST_KEY) : faultyEncode(GeneralConstants.SAML_RESPONSE_KEY), "SAMLDOC1",
                    asRequest ? faultyEncode(GeneralConstants.SAML_REQUEST_KEY) : faultyEncode(GeneralConstants.SAML_RESPONSE_KEY), "SAMLDOC2",
            };
        } else {
            samlParams = new String[] {asRequest ? GeneralConstants.SAML_REQUEST_KEY : GeneralConstants.SAML_RESPONSE_KEY, "SAMLDOC1"};
        }
        return samlParams;
    }

    private String[] generategenerateDoubledSamlParamsWithRelayState(boolean asRequest, boolean faultyEncode) {
        String[] part1 = generateDoubledSamlParams(asRequest, faultyEncode);
        String[] part2 = new String[] {
                GeneralConstants.RELAY_STATE, "RS2"
        };
        String[] result = Arrays.copyOf(part1, part1.length + part2.length);
        System.arraycopy(part2, 0, result, part1.length, part2.length);
        return result;
    }

    private void checkRedirectUriOperation(TestSAML2BindingBuilder.RedirectBindingBuilder builder, String[] samlParams, boolean asRequest, boolean faultyEncode) throws Exception {

        StringBuffer sb = new StringBuffer("http://127.0.0.1:8080/acs?");
        for (int i = 0; i < samlParams.length; i+=2) {
            if (i > 0) sb.append("&");
            sb.append(samlParams[i]).append("=").append(samlParams[i+1]);
        }

        String redirectUri1;
        if (asRequest) {
            redirectUri1 = builder.request(sb.toString());
        } else {
            redirectUri1 = builder.response(sb.toString());
        }

        MultivaluedHashMap<String, String> queryPars = UriUtils.parseQueryParameters(redirectUri1.substring(redirectUri1.indexOf("?") + 1), true);
        Assert.assertEquals(1, queryPars.get(JBossSAMLConstants.RELAY_STATE.get()).size());
        Assert.assertEquals("RS1", queryPars.get(JBossSAMLConstants.RELAY_STATE.get()).get(0));

        String samlParameterName = asRequest ? GeneralConstants.SAML_REQUEST_KEY : GeneralConstants.SAML_RESPONSE_KEY;

        // check if the saml doc is only one and different
        Assert.assertEquals(1, queryPars.get(samlParameterName).size());
        Assert.assertNotEquals("SAMLDOC1", queryPars.get(samlParameterName).get(0));

        // check if RelayState is still the same as in original builder
        String uri2 = String.format("http://127.0.0.1:8080/acs?%s=SAMLDOC2&RelayState=NEW_RELAY", samlParameterName);
        String redirectUri2;
        if (asRequest) {
            redirectUri2 = builder.request(uri2);
        } else {
            redirectUri2 = builder.response(uri2);
        }
        queryPars = UriUtils.parseQueryParameters(redirectUri2.substring(redirectUri2.indexOf("?") + 1), true);
        Assert.assertEquals(1, queryPars.get(JBossSAMLConstants.RELAY_STATE.get()).size());
        Assert.assertEquals("RS1", queryPars.get(JBossSAMLConstants.RELAY_STATE.get()).get(0));

        // check if the saml doc is only one and different
        Assert.assertEquals(1, queryPars.get(samlParameterName).size());
        Assert.assertNotEquals("SAMLDOC2", queryPars.get(samlParameterName).get(0));

    }

}
