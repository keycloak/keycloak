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

package org.keycloak.common.crypto;


import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.common.util.PemUtils;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:pnalyvayko@agi.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 7/30/2016
 */

public abstract class UserIdentityExtractorProvider {

    private static final Logger logger = Logger.getLogger(UserIdentityExtractorProvider.class);

    public abstract class  SubjectAltNameExtractor implements UserIdentityExtractor {

    }

    public abstract class X500NameRDNExtractor implements UserIdentityExtractor {
    }

    protected class OrExtractor implements UserIdentityExtractor {

        UserIdentityExtractor extractor;
        UserIdentityExtractor other;
        OrExtractor(UserIdentityExtractor extractor, UserIdentityExtractor other) {
            this.extractor = extractor;
            this.other = other;

            if (this.extractor == null)
                throw new IllegalArgumentException("extractor is null");
            if (this.other == null)
                throw new IllegalArgumentException("other is null");
        }

        @Override
        public Object extractUserIdentity(X509Certificate[] certs) {
            Object result = this.extractor.extractUserIdentity(certs);
            if (result == null)
                result = this.other.extractUserIdentity(certs);
            return result;
        }
    }

    public class PatternMatcher implements UserIdentityExtractor {
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

    public class OrBuilder {
        UserIdentityExtractor extractor;
        UserIdentityExtractor other;
        OrBuilder(UserIdentityExtractor extractor) {
            this.extractor = extractor;
        }

        public UserIdentityExtractor or(UserIdentityExtractor other) {
            return new OrExtractor(extractor, other);
        }
    }

    public OrBuilder either(UserIdentityExtractor extractor) {
        return new OrBuilder(extractor);
    }
    
    public UserIdentityExtractor getCertificatePemIdentityExtractor() {
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

    public UserIdentityExtractor getPatternIdentityExtractor(String pattern,
                                                                 Function<X509Certificate[],String> valueToMatch) {
                                                                     return new PatternMatcher(pattern, valueToMatch);
                                                                 }

    public abstract UserIdentityExtractor getX500NameExtractor(String identifier, Function<X509Certificate[],Principal> x500Name);

    /**
     * Obtains the subjectAltName given a <code>generalName</code>.
     *
     * @param generalName an integer representing the general name. See {@link X509Certificate#getSubjectAlternativeNames()}
     * @return the value from the subjectAltName extension
     */
    public abstract SubjectAltNameExtractor getSubjectAltNameExtractor(int generalName);
}
