/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.x509;

import freemarker.template.utility.NullArgumentException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.services.ServicesLogger;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:pnalyvayko@agi.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 7/30/2016
 */

public abstract class UserIdentityExtractor {

    private static final ServicesLogger logger = ServicesLogger.LOGGER;

    public abstract Object extractUserIdentity(X509Certificate[] certs);

    static class OrExtractor extends UserIdentityExtractor {

        UserIdentityExtractor extractor;
        UserIdentityExtractor other;
        OrExtractor(UserIdentityExtractor extractor, UserIdentityExtractor other) {
            this.extractor = extractor;
            this.other = other;

            if (this.extractor == null)
                throw new NullArgumentException("extractor");
            if (this.other == null)
                throw new NullArgumentException("other");
        }

        @Override
        public Object extractUserIdentity(X509Certificate[] certs) {
            Object result = this.extractor.extractUserIdentity(certs);
            if (result == null)
                result = this.other.extractUserIdentity(certs);
            return result;
        }
    }

    static class X500NameRDNExtractor extends UserIdentityExtractor {

        private ASN1ObjectIdentifier x500NameStyle;
        Function<X509Certificate[],X500Name> x500Name;
        X500NameRDNExtractor(ASN1ObjectIdentifier x500NameStyle, Function<X509Certificate[],X500Name> x500Name) {
            this.x500NameStyle = x500NameStyle;
            this.x500Name = x500Name;
        }

        @Override
        public Object extractUserIdentity(X509Certificate[] certs) {

            if (certs == null || certs.length == 0)
                throw new IllegalArgumentException();

            X500Name name = x500Name.apply(certs);
            if (name != null) {
                RDN[] rnds = name.getRDNs(x500NameStyle);
                if (rnds != null && rnds.length > 0) {
                    RDN cn = rnds[0];
                    return IETFUtils.valueToString(cn.getFirst().getValue());
                }
            }
            return null;
        }
    }

    /**
     * Extracts the subject identifier from the subjectAltName extension.
     */
    static class SubjectAltNameExtractor extends UserIdentityExtractor {

        // User Principal Name. Used typically by Microsoft in certificates for Smart Card Login
        private static final String UPN_OID = "1.3.6.1.4.1.311.20.2.3";

        private final int generalName;

        /**
         * Creates a new instance
         *
         * @param generalName an integer representing the general name. See {@link X509Certificate#getSubjectAlternativeNames()}
         */
        SubjectAltNameExtractor(int generalName) {
            this.generalName = generalName;
        }

        @Override
        public Object extractUserIdentity(X509Certificate[] certs) {
            if (certs == null || certs.length == 0) {
                throw new IllegalArgumentException();
            }

            try {
                Collection<List<?>> subjectAlternativeNames = certs[0].getSubjectAlternativeNames();

                if (subjectAlternativeNames == null) {
                    return null;
                }

                Iterator<List<?>> iterator = subjectAlternativeNames.iterator();

                boolean foundUpn = false;
                String tempOtherName = null;
                String tempOid = null;

                while (iterator.hasNext() && !foundUpn) {
                    List<?> next = iterator.next();

                    if (Integer.class.cast(next.get(0)) == generalName) {

                        // We will try to find UPN_OID among the subjectAltNames of type 'otherName' . Just if not found, we will fallback to the other type
                        for (int i = 1 ; i<next.size() ; i++) {
                            Object obj = next.get(i);

                            // We have Subject Alternative Name of other type than 'otherName' . Just return it directly
                            if (generalName != 0) {
                                logger.tracef("Extracted identity '%s' from Subject Alternative Name of type '%d'", obj, generalName);
                                return obj;
                            }

                            byte[] otherNameBytes = (byte[]) obj;

                            try {
                                ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(otherNameBytes));
                                ASN1Encodable asn1otherName = asn1Stream.readObject();
                                asn1otherName = unwrap(asn1otherName);

                                ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(asn1otherName);

                                if (asn1Sequence != null) {
                                    ASN1Encodable encodedOid = asn1Sequence.getObjectAt(0);
                                    ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(unwrap(encodedOid));
                                    tempOid = oid.getId();

                                    ASN1Encodable principalNameEncoded = asn1Sequence.getObjectAt(1);
                                    DERUTF8String principalName = DERUTF8String.getInstance(unwrap(principalNameEncoded));

                                    tempOtherName = principalName.getString();

                                    // We found UPN among the 'otherName' principal. We don't need to look other
                                    if (UPN_OID.equals(tempOid)) {
                                        foundUpn = true;
                                        break;
                                    }
                                }

                            } catch (Exception e) {
                                logger.error("Failed to parse subjectAltName", e);
                            }
                        }

                    }
                }

                logger.tracef("Parsed otherName from subjectAltName. OID: '%s', Principal: '%s'", tempOid, tempOtherName);

                return tempOtherName;

            } catch (CertificateParsingException cause) {
                logger.errorf(cause, "Failed to obtain identity from subjectAltName extension");
            }

            return null;
        }


        private ASN1Encodable unwrap(ASN1Encodable encodable) {
            while (encodable instanceof ASN1TaggedObject) {
                ASN1TaggedObject taggedObj = (ASN1TaggedObject) encodable;
                encodable = taggedObj.getObject();
            }

            return encodable;
        }
    }

    static class PatternMatcher extends UserIdentityExtractor {
        private final String _pattern;
        private final Function<X509Certificate[],String> _f;
        PatternMatcher(String pattern, Function<X509Certificate[],String> valueToMatch) {
            _pattern = pattern;
            _f = valueToMatch;
        }

        @Override
        public Object extractUserIdentity(X509Certificate[] certs) {
            String value = Optional.ofNullable(_f.apply(certs)).orElseThrow(IllegalArgumentException::new);

            Pattern r = Pattern.compile(_pattern, Pattern.CASE_INSENSITIVE);

            Matcher m = r.matcher(value);

            if (!m.find()) {
                logger.debugf("[PatternMatcher:extract] No matches were found for input \"%s\", pattern=\"%s\"", value, _pattern);
                return null;
            }

            if (m.groupCount() != 1) {
                logger.debugf("[PatternMatcher:extract] Match produced more than a single group for input \"%s\", pattern=\"%s\"", value, _pattern);
                return null;
            }

            return m.group(1);
        }
    }

    static class OrBuilder {
        UserIdentityExtractor extractor;
        UserIdentityExtractor other;
        OrBuilder(UserIdentityExtractor extractor) {
            this.extractor = extractor;
        }

        public UserIdentityExtractor or(UserIdentityExtractor other) {
            return new OrExtractor(extractor, other);
        }
    }

    public static UserIdentityExtractor getPatternIdentityExtractor(String pattern,
                                                                 Function<X509Certificate[],String> func) {
        return new PatternMatcher(pattern, func);
    }

    public static UserIdentityExtractor getX500NameExtractor(ASN1ObjectIdentifier identifier, Function<X509Certificate[],X500Name> x500Name) {
        return new X500NameRDNExtractor(identifier, x500Name);
    }

    /**
     * Obtains the subjectAltName given a <code>generalName</code>.
     *
     * @param generalName an integer representing the general name. See {@link X509Certificate#getSubjectAlternativeNames()}
     * @return the value from the subjectAltName extension
     */
    public static SubjectAltNameExtractor getSubjectAltNameExtractor(int generalName) {
        return new SubjectAltNameExtractor(generalName);
    }

    public static OrBuilder either(UserIdentityExtractor extractor) {
        return new OrBuilder(extractor);
    }

    public static UserIdentityExtractor getCertificatePemIdentityExtractor(X509AuthenticatorConfigModel config) {
        return new UserIdentityExtractor() {
              @Override
              public Object extractUserIdentity(X509Certificate[] certs) {
                if (certs == null || certs.length == 0) {
                  throw new IllegalArgumentException();
                }

                String pem = PemUtils.encodeCertificate(certs[0]);
                logger.debugf("Using PEM certificate \"%s\" as user identity.", pem);
                return pem;
              }
        };
    }
}
