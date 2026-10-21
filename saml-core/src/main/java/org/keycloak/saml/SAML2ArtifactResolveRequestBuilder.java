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

package org.keycloak.saml;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;

import org.w3c.dom.Document;

public class SAML2ArtifactResolveRequestBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2ArtifactResolveRequestBuilder> {
    protected String artifact;
    protected String destination;
    protected NameIDType issuer;
    protected final List<NodeGenerator> extensions = new LinkedList<>();

    public SAML2ArtifactResolveRequestBuilder artifact(String artifact) {
        this.artifact = artifact;
        return this;
    }

    public SAML2ArtifactResolveRequestBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2ArtifactResolveRequestBuilder issuer(NameIDType issuer) {
        this.issuer = issuer;
        return this;
    }

    public SAML2ArtifactResolveRequestBuilder issuer(String issuer) {
        return issuer(SAML2NameIDBuilder.value(issuer).build());
    }

    @Override
    public SAML2ArtifactResolveRequestBuilder addExtension(NodeGenerator extension) {
        this.extensions.add(extension);
        return this;
    }

    public Document buildDocument() throws ProcessingException, ConfigurationException, ParsingException {
        Document document = SAML2Request.convert(createArtifactResolveRequest());
        return document;
    }

    public ArtifactResolveType createArtifactResolveRequest() throws ConfigurationException {
        ArtifactResolveType lort = SAML2Request.createArtifactResolveRequest(issuer);

        lort.setIssuer(issuer);

        if (destination != null) {
            lort.setDestination(URI.create(destination));
        }

        if (artifact != null) {
            lort.setArtifact(artifact);
        }

        if (!this.extensions.isEmpty()) {
            ExtensionsType extensionsType = new ExtensionsType();
            for (NodeGenerator extension : this.extensions) {
                extensionsType.addExtension(extension);
            }
            lort.setExtensions(extensionsType);
        }

        return lort;
    }
}