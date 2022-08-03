package org.keycloak.crypto.fips.test;

import org.junit.Before;
import org.junit.Assume;
import org.keycloak.KeyPairVerifierTest;
import org.keycloak.common.util.Environment;

/**
 * Test with fips1402 security provider and bouncycastle-fips
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402KeyPairVerifierTest extends KeyPairVerifierTest {

    @Before
    public void initPrivKeys() {

        // The parent private key is not supported in FIPS-140-2, using a PKCS#8 formatted key
        privateKey1 = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKtWsK5O0CtuBpnM" +
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

        // The parent private key is not supported in FIPS-140-2, using a PKCS#8 formatted key
        privateKey2048 = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDhXcyk6e4qx0Ft" +
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

    }

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }
}
