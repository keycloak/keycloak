/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.validators;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check that Destination field in SAML request/response is either unset or matches the expected one.
 * @author hmlnarik
 */
public class DestinationValidator {

    private static final Pattern PROTOCOL_MAP_PATTERN = Pattern.compile("\\s*([a-zA-Z][a-zA-Z\\d+-.]*)\\s*=\\s*(\\d+)\\s*");
    private static final String[] DEFAULT_PROTOCOL_TO_PORT_MAP = new String[] { "http=80", "https=443" };

    private final Map<String, Integer> knownPorts;
    private final Map<Integer, String> knownProtocols;

    private DestinationValidator(Map<String, Integer> knownPorts, Map<Integer, String> knownProtocols) {
        this.knownPorts = knownPorts;
        this.knownProtocols = knownProtocols;
    }
    
    public static DestinationValidator forProtocolMap(String[] protocolMappings) {
        if (protocolMappings == null) {
            protocolMappings = DEFAULT_PROTOCOL_TO_PORT_MAP;
        }

        Map<String, Integer> knownPorts = new HashMap<>();
        Map<Integer, String> knownProtocols = new HashMap<>();

        for (String protocolMapping : protocolMappings) {
            Matcher m = PROTOCOL_MAP_PATTERN.matcher(protocolMapping);
            if (m.matches()) {
                Integer port = Integer.valueOf(m.group(2));
                String proto = m.group(1);

                knownPorts.put(proto, port);
                knownProtocols.put(port, proto);
            }
        }

        return new DestinationValidator(knownPorts, knownProtocols);
    }

    public boolean validate(String expectedDestination, String actualDestination) {
        try {
            return validate(expectedDestination == null ? null : URI.create(expectedDestination), actualDestination);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean validate(String expectedDestination, URI actualDestination) {
        try {
            return validate(expectedDestination == null ? null : URI.create(expectedDestination), actualDestination);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean validate(URI expectedDestination, String actualDestination) {
        try {
            return validate(expectedDestination, actualDestination == null ? null : URI.create(actualDestination));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean validate(URI expectedDestination, URI actualDestination) {
        if (actualDestination == null) {
            return true;    // destination is optional
        }

        if (expectedDestination == null) {
            return false;    // expected destination is mandatory
        }

        if (Objects.equals(expectedDestination, actualDestination)) {
            return true;
        }

        Integer portByScheme = knownPorts.get(expectedDestination.getScheme());
        String protocolByPort = knownProtocols.get(expectedDestination.getPort());

        URI updatedUri = null;
        try {
            if (expectedDestination.getPort() < 0 && portByScheme != null) {
                updatedUri = new URI(
                  expectedDestination.getScheme(),
                  expectedDestination.getUserInfo(),
                  expectedDestination.getHost(),
                  portByScheme,
                  expectedDestination.getPath(),
                  expectedDestination.getQuery(),
                  expectedDestination.getFragment()
                );
            } else if (expectedDestination.getPort() >= 0 && Objects.equals(protocolByPort, expectedDestination.getScheme())) {
                updatedUri = new URI(
                  expectedDestination.getScheme(),
                  expectedDestination.getUserInfo(),
                  expectedDestination.getHost(),
                  -1,
                  expectedDestination.getPath(),
                  expectedDestination.getQuery(),
                  expectedDestination.getFragment()
                );
            }
        } catch (URISyntaxException ex) {
            return false;
        }

        return Objects.equals(updatedUri, actualDestination);
    }

}
