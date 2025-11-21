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

package org.keycloak.jose.jws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.JOSE;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JWSInput implements JOSE {
    String wireString;
    String encodedHeader;
    String encodedContent;
    String encodedSignature;
    String encodedSignatureInput;
    JWSHeader header;
    byte[] content;
    byte[] signature;


    public JWSInput(String wire) throws JWSInputException {
        try {
            this.wireString = wire;
            String[] parts = wire.split("\\.");
            if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Parsing error");
            encodedHeader = parts[0];
            encodedContent = parts[1];
            encodedSignatureInput = encodedHeader + '.' + encodedContent;
            content = Base64Url.decode(encodedContent);
            if (parts.length > 2) {
                encodedSignature = parts[2];
                signature = Base64Url.decode(encodedSignature);

            }
            byte[] headerBytes = Base64Url.decode(encodedHeader);
            header = JsonSerialization.readValue(headerBytes, JWSHeader.class);
        } catch (Throwable t) {
            throw new JWSInputException(t);
        }
    }

    public String getWireString() {
        return wireString;
    }

    public String getEncodedHeader() {
        return encodedHeader;
    }

    public String getEncodedContent() {
        return encodedContent;
    }

    public String getEncodedSignature() {
        return encodedSignature;
    }
    public String getEncodedSignatureInput() {
        return encodedSignatureInput;
    }

    public JWSHeader getHeader() {
        return header;
    }

    public byte[] getContent() {
        return content;
    }

    public byte[] getSignature() {
        return signature;
    }

    public <T> T readJsonContent(Class<T> type) throws JWSInputException {
        try {
            return JsonSerialization.readValue(content, type);
        } catch (IOException e) {
            throw new JWSInputException(e);
        }
    }

    public String readContentAsString() {
        return new String(content, StandardCharsets.UTF_8);
    }
}
