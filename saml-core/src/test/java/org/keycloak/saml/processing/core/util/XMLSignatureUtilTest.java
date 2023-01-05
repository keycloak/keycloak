/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.saml.processing.core.util;

import java.security.Key;
import java.security.Security;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.PemUtils;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.w3c.dom.Document;

/**
 * Test for the illustration purpose, which fails on OpenJDK17 due the fact that algorithm "http://www.w3.org/2000/09/xmldsig#rsa-sha1" is disallowed on OpenJDK 17.
 *
 * It is possible to make test working on this platform either by:
 * - Update $JAVA_HOME/conf/security/java.security and remove line "disallowAlg http://www.w3.org/2000/09/xmldsig#rsa-sha1" from the property "jdk.xml.dsig.secureValidationPolicy"
 * - Uncomment line in XMLSignatureUtil.validateUsingKeySelector and add this property to the validation context before signature validation: valContext.setProperty("org.jcp.xml.dsig.secureValidation", false);
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class XMLSignatureUtilTest {

    @Test
    public void testMe() throws Exception {
        //Security.setProperty("org.jcp.xml.dsig.secureValidation", false);

        CryptoIntegration.init(this.getClass().getClassLoader());
        String samlRequest = "PHNhbWxwOkxvZ291dFJlcXVlc3QgeG1sbnM6c2FtbHA9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpwcm90b2NvbCIgeG1sbnM9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIERlc3RpbmF0aW9uPSJodHRwOi8vbG9jYWxob3N0OjgyODAvZW1wbG95ZWUtc2lnLWZyb250L3NhbWwiIElEPSJJRF85MTJiNTFiYi00Nzg3LTQ1NTAtOGQ5MS0xYzg0ZmZhOGI1NzciIElzc3VlSW5zdGFudD0iMjAyMy0wMS0wNFQxNToyOTowMS42NDdaIiBWZXJzaW9uPSIyLjAiPjxzYW1sOklzc3Vlcj5odHRwczovL2xvY2FsaG9zdDo4NTQzL2F1dGgvcmVhbG1zL2RlbW88L3NhbWw6SXNzdWVyPjxkc2lnOlNpZ25hdHVyZSB4bWxuczpkc2lnPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj48ZHNpZzpTaWduZWRJbmZvPjxkc2lnOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48ZHNpZzpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz48ZHNpZzpSZWZlcmVuY2UgVVJJPSIjSURfOTEyYjUxYmItNDc4Ny00NTUwLThkOTEtMWM4NGZmYThiNTc3Ij48ZHNpZzpUcmFuc2Zvcm1zPjxkc2lnOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIi8+PGRzaWc6VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8+PC9kc2lnOlRyYW5zZm9ybXM+PGRzaWc6RGlnZXN0TWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3NoYTEiLz48ZHNpZzpEaWdlc3RWYWx1ZT5pN1pkWk51MC9yQTZ2eGo1V1BsZHVTa282bFE9PC9kc2lnOkRpZ2VzdFZhbHVlPjwvZHNpZzpSZWZlcmVuY2U+PC9kc2lnOlNpZ25lZEluZm8+PGRzaWc6U2lnbmF0dXJlVmFsdWU+TFQ1WmRxZHJHOGF0V0NVdUY5OEVWcVI2REtVTU1QZDM0NXN4bHhDeU56b2FjZllndURhL1MxcjVnME13M3hXTStCU3ZSYlgveTJJNiYjMTM7ClNtcU9XM3BUVk1yWjJTUS9tK2x2eFJ2YndqWllib2VCSGx0Q0pnMjdLTWlGZGxJUm5SQi8zVkFSUHFMWVZuckxFaFVIcS9qd1RpTEsmIzEzOwpmSStHT05sQnFxV3d2QXRXK0x3PTwvZHNpZzpTaWduYXR1cmVWYWx1ZT48ZHNpZzpLZXlJbmZvPjxkc2lnOktleU5hbWU+Rko4NkdjRjNqVGJOTE9jbzROdlprVUNJVW1mWUNxb3F0T1FlTWZiaE5sRTwvZHNpZzpLZXlOYW1lPjxkc2lnOlg1MDlEYXRhPjxkc2lnOlg1MDlDZXJ0aWZpY2F0ZT5NSUlCa1RDQit3SUdBWVY5WHRCRE1BMEdDU3FHU0liM0RRRUJDd1VBTUE4eERUQUxCZ05WQkFNTUJHUmxiVzh3SGhjTk1qTXdNVEEwJiMxMzsKTVRVeE9UQXpXaGNOTXpNd01UQTBNVFV5TURReldqQVBNUTB3Q3dZRFZRUUREQVJrWlcxdk1JR2ZNQTBHQ1NxR1NJYjNEUUVCQVFVQSYjMTM7CkE0R05BRENCaVFLQmdRQ3JWckN1VHRBcmJnYVp6TDFodmgweHRMNW1jN28wTnFQVm5ZWGtMdmdjd2lDM0JqTEd3MXRHRUdvSmFYRHUmIzEzOwpTYVJsbG9ibTUzSkJoangzM1VOdis1ei9VTUc0a3l0Qld4aGVOVktuTDZHZ3FsTmFiTWFGZlBMUENGOGtBZ0tuc2k3OU5NbytuNktuJiMxMzsKU1k4WWVVbWVjL3AydmpPMk5qc1NBVmNXRVFNVmhKMzFMd0lEQVFBQk1BMEdDU3FHU0liM0RRRUJDd1VBQTRHQkFJSUVPbFN6bmx0aCYjMTM7CnMwWnZJLzJnd09ZcjVVZlFLOXg3WVkxTWdEanMvUjhNWHFIcXRGS1hHVTVzaEhoWkc3YUgvZUZXMExvV29LeklOWGIxL2ZXblNZZ1gmIzEzOwp1L2tzd21RQ1pNTkE4T2laSHpPTXBPL3RzWFZ4U05zU2xWbnZ4VWx4VzZIM0s3VGlQWlB2aUtZc0Zoem9EdzJBNG8wcXRqM01OMDRFJiMxMzsKNTdMZk1ieUE8L2RzaWc6WDUwOUNlcnRpZmljYXRlPjwvZHNpZzpYNTA5RGF0YT48ZHNpZzpLZXlWYWx1ZT48ZHNpZzpSU0FLZXlWYWx1ZT48ZHNpZzpNb2R1bHVzPnExYXdyazdRSzI0R21jeTlZYjRkTWJTK1puTzZORGFqMVoyRjVDNzRITUlndHdZeXhzTmJSaEJxQ1dsdzdrbWtaWmFHNXVkeVFZWTgmIzEzOwpkOTFEYi91Yy8xREJ1Sk1yUVZzWVhqVlNweStob0twVFdtekdoWHp5endoZkpBSUNwN0l1L1RUS1BwK2lwMG1QR0hsSm5uUDZkcjR6JiMxMzsKdGpZN0VnRlhGaEVERllTZDlTOD08L2RzaWc6TW9kdWx1cz48ZHNpZzpFeHBvbmVudD5BUUFCPC9kc2lnOkV4cG9uZW50PjwvZHNpZzpSU0FLZXlWYWx1ZT48L2RzaWc6S2V5VmFsdWU+PC9kc2lnOktleUluZm8+PC9kc2lnOlNpZ25hdHVyZT48c2FtbDpOYW1lSUQgRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoxLjE6bmFtZWlkLWZvcm1hdDp1bnNwZWNpZmllZCI+dW5hdXRob3JpemVkPC9zYW1sOk5hbWVJRD48c2FtbHA6U2Vzc2lvbkluZGV4PjQ2MzQ1ODI3LTA2Y2UtNDM2ZC04YTA1LTAzN2QwNjExN2MwYzo6NzU3N2VkMDUtNTNmZS00ZDNmLThkOGEtZjdlNDlhYWQ2NDY3PC9zYW1scDpTZXNzaW9uSW5kZXg+PC9zYW1scDpMb2dvdXRSZXF1ZXN0Pg==";
        String publicKeyPem = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

        SAMLDocumentHolder holder = SAMLRequestParser.parseRequestPostBinding(samlRequest);
        Document doc = holder.getSamlDocument();

        Key key = PemUtils.decodePublicKey(publicKeyPem);
        HardcodedKeyLocator keyLocator = new HardcodedKeyLocator(key);

        boolean valid = XMLSignatureUtil.validate(doc, keyLocator);

        Assert.assertFalse(valid);
    }
}
