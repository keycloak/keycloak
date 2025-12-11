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

package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;


import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.util.JsonSerialization;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.json.JsonUtils;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.apicatalog.rdf.Rdf;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.io.RdfWriter;
import com.apicatalog.rdf.io.error.RdfWriterException;
import com.apicatalog.rdf.io.error.UnsupportedContentException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.setl.rdf.normalization.RdfNormalize;

/**
 * Implementation of an LD-Crypto Suite for Ed25519Signature2018
 * {@see https://w3c-ccg.github.io/ld-cryptosuite-registry/#ed25519signature2018}
 * <p>
 * Canonicalization Algorithm: https://w3id.org/security#URDNA2015
 * Digest Algorithm: http://w3id.org/digests#sha256
 * Signature Algorithm: http://w3id.org/security#ed25519
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class Ed255192018Suite implements LinkedDataCryptographicSuite {

    private final SignatureSignerContext signerContext;

    public static final String PROOF_TYPE = "Ed25519Signature2018";

    public Ed255192018Suite(SignatureSignerContext signerContext) {
        this.signerContext = signerContext;
    }

    @Override
    public byte[] getSignature(VerifiableCredential verifiableCredential) {
        byte[] transformedData = transform(verifiableCredential);
        byte[] hashedData = digest(transformedData);
        return sign(hashedData);
    }

    private byte[] transform(VerifiableCredential verifiableCredential) {

        try {
            String credentialString = JsonSerialization.mapper.writeValueAsString(verifiableCredential);

            var credentialDocument = JsonDocument.of(new StringReader(credentialString));

            var expandedDocument = JsonLd.expand(credentialDocument)
                    .loader(new HttpLoader(DefaultHttpClient.defaultInstance()))
                    .get();
            Optional<JsonObject> documentObject = Optional.empty();
            if (JsonUtils.isArray(expandedDocument)) {
                documentObject = expandedDocument.asJsonArray().stream().filter(JsonUtils::isObject).map(JsonValue::asJsonObject).findFirst();
            } else if (JsonUtils.isObject(expandedDocument)) {
                documentObject = Optional.of(expandedDocument.asJsonObject());
            }
            if (documentObject.isPresent()) {

                RdfDataset rdfDataset = JsonLd.toRdf(JsonDocument.of(documentObject.get())).get();
                RdfDataset canonicalDataset = RdfNormalize.normalize(rdfDataset);

                StringWriter writer = new StringWriter();
                RdfWriter rdfWriter = Rdf.createWriter(MediaType.N_QUADS, writer);
                rdfWriter.write(canonicalDataset);

                return writer.toString()
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                throw new CredentialSignerException("Was not able to get the expanded json.");
            }
        } catch (JsonProcessingException e) {
            throw new CredentialSignerException("Was not able to serialize the credential", e);
        } catch (JsonLdError e) {
            throw new CredentialSignerException("Was not able to create a JsonLD Document from the serialized string.", e);
        } catch (UnsupportedContentException | IOException | RdfWriterException e) {
            throw new CredentialSignerException("Was not able to canonicalize the json-ld.", e);
        }

    }

    private byte[] digest(byte[] transformedData) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(transformedData);
        } catch (NoSuchAlgorithmException e) {
            throw new CredentialSignerException("Algorithm SHA-256 not supported.", e);
        }
    }

    private byte[] sign(byte[] hashData) {
        return signerContext.sign(hashData);
    }


    @Override
    public String getProofType() {
        return PROOF_TYPE;
    }
}
