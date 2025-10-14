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

package org.keycloak;

import org.keycloak.common.VerificationException;
import org.keycloak.rule.CryptoInitRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * This is not tested in keycloak-core. The subclasses should be created in the crypto modules to make sure it is tested with corresponding modules (bouncycastle VS bouncycastle-fips)
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class KeyPairVerifierTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    protected String privateKey1 = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    String publicKey1 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    protected String privateKey2048 = "-----BEGIN RSA PRIVATE KEY-----\n" + "MIIEpQIBAAKCAQEA4V3MpOnuKsdBbR1UzNjK9o5meEMQ4s5Vpykhv1DpqTilKOiE\n"
            + "H7VQ/XtjNxw0yjnFBilCnpK6yN9mDEHbBEzaRjtdrgVhkIejiaXFBP5MBhUQ5l9u\n" + "8E3IZC3E8pwDjVF0Z9u0R4lGeUg2k6O+NKumqIvxoLCTuG0zf53bctGsRd57LuFi\n"
            + "pgCkNyxvscOhulsbEMYrLwlb5bMGgx9v+RCnwvunNEb7RK+5pzP+iH1MRejRsX+U\n" + "7h9zHRn2gQhIl7SzG9GXebuPWr4KKwfMHWy0PEuQrsfWRXm9/dTEavbfNkv5E53z\n"
            + "WXjWyf93ezkVhBX0YoXmf6UO7PAlvsrjno3TuwIDAQABAoIBAQC5iCAOcCtLemhp\n" + "bOlADwXgPtErFoNTROyMxjbrKrCCSIjniawj8oAvfiHq38Sx6ydBcDxREZjF/+wi\n"
            + "ESE+hAp6ISt5NSLh+lhu3FK7TqLFqxgTn+NT36Umm+t0k231LGa5jcz3y5KCDCoq\n" + "F3ZiJCH6xeLxGA00mmn4GLvt5aF+jiO80ICGs4iUg99IoXhc5u/VU0hB5J78BinW\n"
            + "inkCABuBNkDLgIqc9BoH4L5MOx3zDqzmHffeq9+2V4X7NiD5QyiyWtABaQpEIY5k\n" + "R48RTno6xN3hvG48/DwkO2gABSLQ/OJd3Hupv4wlmmSc1xo93CaV44hq2i2GsU1i\n"
            + "m6d3xDW5AoGBAPCfkvPkqr88xg+8Cu3G/3GFpUsQ0VEme+8dIjXMTJHa13K7xaRh\n" + "GHCVg4a8oHJ/P/vNSwvPyR71iRX4csqkKSaprvJk8vxbU539unmHWKkfUHrywQlz\n"
            + "q4OuXOjOdvILLOTsu3/+k6vAIE6SZJiDmf2eGxi9Qbm5rlxE3h3HRAKfAoGBAO/E\n" + "ogHV86LmnJTJbx1hP3IfRHk0qaiSj35ljlAz+3v6GN/KSUYCWTtp2GjRIKY3qQ8I\n"
            + "7l+PVTFg3SY7cPq2C9TE+6xroiWkUd2JldPLYSxpWpFNYlo709SzmLquDho+fwJC\n" + "nAxoxKghsXJarz7TRfNyFqDXscS6oQLurU9P5lVlAoGBAJh1QvLtS5Jnu0Z06qfF\n"
            + "kkwnVZe+TCGStKvIVciobUts0V2Mw6lnK8kJspBIK5DgN3YfmREe0lufTwBwrqre\n" + "YIRytro2ZA6o/s332ZLuwqpFgQSlktGeTGnerFeFma+6jPNvW025y27jCJVABCTu\n"
            + "HT+oUZrXLzGyCFvF9sX/X4QZAoGBAICap4r0h0nJCBOGN+M6Vh2QR9n7NUUF15Gk\n" + "R0EdoLZO3yiqB8NVXydPDpSqFykQkc1OrQz0hG2H1xa6q07OdmoZfiRtVvt5t69s\n"
            + "LMD9RZHcsIdfSnG7xVNBQZpf4ZCSFO3RbIH7b//+kn8TxQudptd9SkXba65prBM2\n" + "kh8IbDNBAoGAVsKvkruH7RK7CimDSWcdAKvHARqkjs/PoeKEEY8Yu6zf0Z9TQM5l\n"
            + "uC9EwBamYcSusWRcdcz+9HYG58XFnmXq+3EUuFbJ+Ljb8YWBgePjSHDoS/6+/+zq\n" + "B1b5uQp/jYFbYQl50UPRPTF+ul1eQoy7F43Ngj3/5cDRarFZe3ZTzZo=\n"
            + "-----END RSA PRIVATE KEY-----";

    String publicKey2048 = "-----BEGIN PUBLIC KEY-----\n" + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4V3MpOnuKsdBbR1UzNjK\n"
            + "9o5meEMQ4s5Vpykhv1DpqTilKOiEH7VQ/XtjNxw0yjnFBilCnpK6yN9mDEHbBEza\n" + "RjtdrgVhkIejiaXFBP5MBhUQ5l9u8E3IZC3E8pwDjVF0Z9u0R4lGeUg2k6O+NKum\n"
            + "qIvxoLCTuG0zf53bctGsRd57LuFipgCkNyxvscOhulsbEMYrLwlb5bMGgx9v+RCn\n" + "wvunNEb7RK+5pzP+iH1MRejRsX+U7h9zHRn2gQhIl7SzG9GXebuPWr4KKwfMHWy0\n"
            + "PEuQrsfWRXm9/dTEavbfNkv5E53zWXjWyf93ezkVhBX0YoXmf6UO7PAlvsrjno3T\n" + "uwIDAQAB\n" + "-----END PUBLIC KEY-----";

    @Test
    public void verifyWith1024PrivateKeyInTraditionalRSAFormat() throws Exception {
        verifyImplRsa1024Key(this.privateKey1);
    }

    @Test
    public void verifyWith2048PrivateKeyInTraditionalRSAFormat() throws Exception {
        verifyImplRsa2048Key(this.privateKey2048);
    }

    @Test
    public void verifyWith1024PrivateKeyInPKCS8Format() throws Exception {
        String privateKey1 = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKtWsK5O0CtuBpnM" +
                "vWG+HTG0vmZzujQ2o9WdheQu+BzCILcGMsbDW0YQaglpcO5JpGWWhubnckGGPHfd" +
                "Q2/7nP9QwbiTK0FbGF41UqcvoaCqU1psxoV88s8IXyQCAqeyLv00yj6foqdJjxh5" +
                "SZ5z+na+M7Y2OxIBVxYRAxWEnfUvAgMBAAECgYB+Y7yBWHIHF2qXGYi6CVvPxtyN" +
                "BuFcktHYShLyeBNeY3VujYv3QzSZQpJ1zuoXXQuARMHOovyNiVAhu357pMfx9wSk" +
                "oKNSXKrQx/+9Vt9lI1pXJxjXedPOjbuI/JZAcrk0u4nOfXG/HGtR5cjoDZYWkYQE" +
                "tsePCnHlZAb0D7axwQJBAO92f00Tvkc9NU/EGqwR3bPXRMqSX0JnG7XRBvLeJBCZ" +
                "YsQn0s2bLdpy8qsTeAyJg1ZvrEc8qIio5HVqzsvbhpMCQQC3K9A6UK+vmQCNWqsQ" +
                "pdqWPRPN7CPB67FzSmyS8CtMjY6jTvSHrkamggotz2N/5QDr1xG2q7A/3dpkq1bT" +
                "pTx1AkAXZjjiSz+Yrn57IOqKTeSgIjTypoLwdirbBWXsbZCQnqxsBogu1y8P3ZOg" +
                "6/IbJ4TR+W+YNnExiW9pmdpDSVxJAkEAplTq6YmLf/F4RuQmox94tyUPbtcYQWg9" +
                "42uZ3HSrXQDOng18kBj5nwpHJAJHYEQb6g2K0E5n5hcX0oKkfdx2YQJAcSKAmFiD" +
                "7KQ6+vVqJlQwVPvYdTSOeZB7YVV6S4b4slS3ZObsa0yNMWgal/QnCtW5k3f185gC" +
                "Wj6dOLGB5btfxg==";

        verifyImplRsa1024Key(privateKey1);
    }

    @Test
    public void verifyWith2048PrivateKeyInPKCS8Format() throws Exception {
        String privateKey2048 = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDhXcyk6e4qx0Ft" +
                "HVTM2Mr2jmZ4QxDizlWnKSG/UOmpOKUo6IQftVD9e2M3HDTKOcUGKUKekrrI32YM" +
                "QdsETNpGO12uBWGQh6OJpcUE/kwGFRDmX27wTchkLcTynAONUXRn27RHiUZ5SDaT" +
                "o740q6aoi/GgsJO4bTN/ndty0axF3nsu4WKmAKQ3LG+xw6G6WxsQxisvCVvlswaD" +
                "H2/5EKfC+6c0RvtEr7mnM/6IfUxF6NGxf5TuH3MdGfaBCEiXtLMb0Zd5u49avgor" +
                "B8wdbLQ8S5Cux9ZFeb391MRq9t82S/kTnfNZeNbJ/3d7ORWEFfRiheZ/pQ7s8CW+" +
                "yuOejdO7AgMBAAECggEBALmIIA5wK0t6aGls6UAPBeA+0SsWg1NE7IzGNusqsIJI" +
                "iOeJrCPygC9+IerfxLHrJ0FwPFERmMX/7CIRIT6ECnohK3k1IuH6WG7cUrtOosWr" +
                "GBOf41PfpSab63STbfUsZrmNzPfLkoIMKioXdmIkIfrF4vEYDTSaafgYu+3loX6O" +
                "I7zQgIaziJSD30iheFzm79VTSEHknvwGKdaKeQIAG4E2QMuAipz0Ggfgvkw7HfMO" +
                "rOYd996r37ZXhfs2IPlDKLJa0AFpCkQhjmRHjxFOejrE3eG8bjz8PCQ7aAAFItD8" +
                "4l3ce6m/jCWaZJzXGj3cJpXjiGraLYaxTWKbp3fENbkCgYEA8J+S8+SqvzzGD7wK" +
                "7cb/cYWlSxDRUSZ77x0iNcxMkdrXcrvFpGEYcJWDhrygcn8/+81LC8/JHvWJFfhy" +
                "yqQpJqmu8mTy/FtTnf26eYdYqR9QevLBCXOrg65c6M528gss5Oy7f/6Tq8AgTpJk" +
                "mIOZ/Z4bGL1BubmuXETeHcdEAp8CgYEA78SiAdXzouaclMlvHWE/ch9EeTSpqJKP" +
                "fmWOUDP7e/oY38pJRgJZO2nYaNEgpjepDwjuX49VMWDdJjtw+rYL1MT7rGuiJaRR" +
                "3YmV08thLGlakU1iWjvT1LOYuq4OGj5/AkKcDGjEqCGxclqvPtNF83IWoNexxLqh" +
                "Au6tT0/mVWUCgYEAmHVC8u1Lkme7RnTqp8WSTCdVl75MIZK0q8hVyKhtS2zRXYzD" +
                "qWcryQmykEgrkOA3dh+ZER7SW59PAHCuqt5ghHK2ujZkDqj+zffZku7CqkWBBKWS" +
                "0Z5Mad6sV4WZr7qM829bTbnLbuMIlUAEJO4dP6hRmtcvMbIIW8X2xf9fhBkCgYEA" +
                "gJqnivSHSckIE4Y34zpWHZBH2fs1RQXXkaRHQR2gtk7fKKoHw1VfJ08OlKoXKRCR" +
                "zU6tDPSEbYfXFrqrTs52ahl+JG1W+3m3r2wswP1Fkdywh19KcbvFU0FBml/hkJIU" +
                "7dFsgftv//6SfxPFC52m131KRdtrrmmsEzaSHwhsM0ECgYBWwq+Su4ftErsKKYNJ" +
                "Zx0Aq8cBGqSOz8+h4oQRjxi7rN/Rn1NAzmW4L0TAFqZhxK6xZFx1zP70dgbnxcWe" +
                "Zer7cRS4Vsn4uNvxhYGB4+NIcOhL/r7/7OoHVvm5Cn+NgVthCXnRQ9E9MX66XV5C" +
                "jLsXjc2CPf/lwNFqsVl7dlPNmg==";

        verifyImplRsa2048Key(privateKey2048);
    }

    protected void verifyImplRsa1024Key(String rsaPrivateKey1024) throws Exception {
        KeyPairVerifier.verify(rsaPrivateKey1024, publicKey1);
        try {
            KeyPairVerifier.verify(rsaPrivateKey1024, publicKey2048);
            Assert.fail("Expected VerificationException");
        } catch (VerificationException e) {
        }
    }

    protected void verifyImplRsa2048Key(String rsaPrivateKey2048) throws Exception {
        KeyPairVerifier.verify(rsaPrivateKey2048, publicKey2048);
        try {
            KeyPairVerifier.verify(rsaPrivateKey2048, publicKey1);
            Assert.fail("Expected VerificationException");
        } catch (VerificationException e) {
        }
    }

}
