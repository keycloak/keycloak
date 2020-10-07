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

package org.keycloak.authentication.authenticators.x509;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.StreamUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SubjectAltNameIdentityExtractorTest {

    @Test
    public void testX509SubjectAltName_otherName() throws Exception {
        UserIdentityExtractor extractor = UserIdentityExtractor.getSubjectAltNameExtractor(0);

        X509Certificate cert = getCertificate();

        Object upn = extractor.extractUserIdentity(new X509Certificate[] { cert});
        Assert.assertEquals("test-user@some-company-domain", upn);
    }


    @Test
    public void testX509SubjectAltName_email() throws Exception {
        UserIdentityExtractor extractor = UserIdentityExtractor.getSubjectAltNameExtractor(1);

        X509Certificate cert = getCertificate();

        Object upn = extractor.extractUserIdentity(new X509Certificate[] { cert});
        Assert.assertEquals("test@somecompany.com", upn);
    }


    private X509Certificate getCertificate() throws Exception {
        InputStream is = getClass().getResourceAsStream("/certs/UPN-cert.pem");

        String s = StreamUtil.readString(is, Charset.defaultCharset());

        return PemUtils.decodeCertificate(s);
    }


}
