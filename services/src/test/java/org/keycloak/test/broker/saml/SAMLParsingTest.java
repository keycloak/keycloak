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

package org.keycloak.test.broker.saml;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.PostBindingUtil;

/**
 * This was failing on IBM JDK
 * 
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SAMLParsingTest {

    private static final String SAML_RESPONSE = "PHNhbWxwOkxvZ291dFJlc3BvbnNlIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIHhtbG5zPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIiBEZXN0aW5hdGlvbj0iaHR0cDovL2xvY2FsaG9zdDo4MDgxL2F1dGgvcmVhbG1zL3JlYWxtLXdpdGgtYnJva2VyL2Jyb2tlci9rYy1zYW1sLWlkcC1iYXNpYy9lbmRwb2ludCIgSUQ9IklEXzlhMTcxZDIzLWM0MTctNDJmNS05YmNhLWMwOTMxMjNmZDY4YyIgSW5SZXNwb25zZVRvPSJJRF9iYzczMDcxMS0yMDM3LTQzZjMtYWQ3Ni03YmMzMzg0MmZiODciIElzc3VlSW5zdGFudD0iMjAxNi0wMi0yOVQxMjowMDoxNC4wNDRaIiBWZXJzaW9uPSIyLjAiPjxzYW1sOklzc3VlciB4bWxuczpzYW1sPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIj5odHRwOi8vbG9jYWxob3N0OjgwODIvYXV0aC9yZWFsbXMvcmVhbG0td2l0aC1zYW1sLWlkcC1iYXNpYzwvc2FtbDpJc3N1ZXI+PHNhbWxwOlN0YXR1cz48c2FtbHA6U3RhdHVzQ29kZSBWYWx1ZT0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnN0YXR1czpTdWNjZXNzIi8+PC9zYW1scDpTdGF0dXM+PC9zYW1scDpMb2dvdXRSZXNwb25zZT4=";

    @Test
    public void parseTest() {
        byte[] samlBytes = PostBindingUtil.base64Decode(SAML_RESPONSE);
        SAMLDocumentHolder holder = SAMLRequestParser.parseResponseDocument(samlBytes);
        Assert.assertNotNull(holder);
    }
}
