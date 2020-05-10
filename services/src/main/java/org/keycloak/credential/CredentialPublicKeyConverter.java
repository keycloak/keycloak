/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.credential;

import com.webauthn4j.converter.util.ObjectConverter;
import org.keycloak.common.util.Base64Url;

import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.data.attestation.authenticator.COSEKey;

public class CredentialPublicKeyConverter {

    private CborConverter cborConverter;

    public CredentialPublicKeyConverter(ObjectConverter objectConverter) {
        this.cborConverter = objectConverter.getCborConverter();
    }

    public String convertToDatabaseColumn(COSEKey credentialPublicKey) {
        return Base64Url.encode(cborConverter.writeValueAsBytes(credentialPublicKey));
    }

    public COSEKey convertToEntityAttribute(String s) {
        return cborConverter.readValue(Base64Url.decode(s), COSEKey.class);
    }
}
