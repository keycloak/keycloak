/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crypto.elytron;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.keycloak.common.crypto.UserIdentityExtractor;
import org.keycloak.common.crypto.UserIdentityExtractorProvider;
import org.wildfly.security.asn1.ASN1;
import org.wildfly.security.asn1.DERDecoder;
import org.wildfly.security.asn1.OidsUtil;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.principal.X500AttributePrincipalDecoder;

/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class ElytronUserIdentityExtractorProvider  extends UserIdentityExtractorProvider {

    private Logger log = Logger.getLogger(this.getClass());

    class X500NameRDNExtractorElytronProvider extends X500NameRDNExtractor {

        private String x500NameStyle;
        Function<X509Certificate[],Principal> x500Name;
        
        public X500NameRDNExtractorElytronProvider(String attrName, Function<X509Certificate[], Principal> x500Name) {
            // The OidsUtil fails to map 'EmailAddress', instead 'E' is mapped to the OID.
            // TODO: Open an issue with wildfly-elytron to include 'EmailAddress' in the oid mapping
            if(attrName.equals("EmailAddress")) {
                attrName = "E";
            }
            this.x500NameStyle = OidsUtil.attributeNameToOid(OidsUtil.Category.RDN, attrName);
            log.debug("Attribute Name: " + attrName + " X500NameStyle OID: " + x500NameStyle);
            this.x500Name = x500Name;
        }

        @Override
        public Object extractUserIdentity(X509Certificate[] certs) {

            if (certs == null || certs.length == 0)
                throw new IllegalArgumentException();

                Principal name = x500Name.apply(certs);
                log.debug("Principal Name " + name.getName());
                X500AttributePrincipalDecoder xDecoder = new X500AttributePrincipalDecoder(x500NameStyle);
                String cn = xDecoder.apply(name);
            
                return cn;
            
        }
    }

    /**
     * Extracts the subject identifier from the subjectAltName extension.
     */
    class SubjectAltNameExtractorEltronProvider extends SubjectAltNameExtractor {

        // User Principal Name. Used typically by Microsoft in certificates for
        // Smart Card Login
        private static final String UPN_OID = "1.3.6.1.4.1.311.20.2.3";

        private final int generalName;

        /**
         * Creates a new instance
         *
         * @param generalName an integer representing the general name. See
         *                    {@link X509Certificate#getSubjectAlternativeNames()}
         */
        SubjectAltNameExtractorEltronProvider(int generalName) {
            this.generalName = generalName;
        }

        @Override
        public Object extractUserIdentity(X509Certificate[] certs) {
            if (certs == null || certs.length == 0) {
                throw new IllegalArgumentException();
            }
            String subjectName = null;

            log.debug("SubjPrinc " + certs[0].getSubjectX500Principal());
            Collection<List<?>> subjectAlternativeNames;
            try {
                subjectAlternativeNames = certs[0].getSubjectAlternativeNames();
                if (subjectAlternativeNames == null) {
                    return null;
                }
                log.info(Arrays.toString(subjectAlternativeNames.toArray()));
                for (List<?> sbjAltName : subjectAlternativeNames) {
                    if (sbjAltName == null)
                        continue;
    
                    Integer nameType = (Integer) sbjAltName.get(0);
    
                    if (nameType == generalName) {
                        Object sbjObj = sbjAltName.get(1);

                        switch (nameType) {
                            case GeneralName.RFC_822_NAME:
                            case GeneralName.DNS_NAME:
                            case GeneralName.DIRECTORY_NAME:
                            case GeneralName.URI_NAME:
                               subjectName = (String) sbjObj;
                               break;
                            case GeneralName.OTHER_NAME:
                            DERDecoder derDecoder = new DERDecoder((byte[])sbjObj);
                            derDecoder.startSequence();
                            boolean upnOidFound = false;
                            while (derDecoder.hasNextElement() && !upnOidFound) {
                                int asn1Type = derDecoder.peekType();
                                log.debug("ASN.1 Type: " + derDecoder.peekType());
                                
                                switch (asn1Type) {
                                    case ASN1.OBJECT_IDENTIFIER_TYPE:
                                        String oid = derDecoder.decodeObjectIdentifier();
                                        log.debug("OID: " + oid);
                                        if(UPN_OID.equals(oid)) {
                                            derDecoder.decodeImplicit(160);
                                            byte[] sb = derDecoder.drainElementValue();
                                            while(!Character.isLetterOrDigit(sb[0])) {
                                                sb = Arrays.copyOfRange(sb, 1, sb.length);
                                            }
                                            subjectName = new String(sb, StandardCharsets.UTF_8);
                                            upnOidFound = true;
                                        }
                                        break;
                                    case ASN1.UTF8_STRING_TYPE:
                                        subjectName = derDecoder.decodeUtf8String();
                                        break;
                                    case ASN1.PRINTABLE_STRING_TYPE:
                                        subjectName = derDecoder.decodePrintableString();
                                        break;
                                    case ASN1.UNIVERSAL_STRING_TYPE:
                                        subjectName = derDecoder.decodeUniversalString();
                                        break;
                                    case ASN1.OCTET_STRING_TYPE:
                                        subjectName = derDecoder.decodeOctetStringAsString();
                                        break;
                                    case 0xa0:
                                        derDecoder.startExplicit(asn1Type);
                                        break;
                                    case ASN1.SEQUENCE_TYPE:
                                        derDecoder.startSequence();
                                    default:
                                        derDecoder.skipElement();

                                }
                                

                            }
                        }
                        
                    }
                    
                }
            } catch (CertificateParsingException e) {
                log.error("Failed to parse Subject Name:",e);
            }
            
            log.debug("Subject Alt Name: " + subjectName);
            return subjectName;
        }
    }
                


    @Override
    public UserIdentityExtractor getX500NameExtractor(String identifier, Function<X509Certificate[], Principal> x500Name) {
        return new X500NameRDNExtractorElytronProvider(identifier, x500Name);
    }

    @Override
    public SubjectAltNameExtractor getSubjectAltNameExtractor(int generalName) {
        return new SubjectAltNameExtractorEltronProvider(generalName);
    }

}
