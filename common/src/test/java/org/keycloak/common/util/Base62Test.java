package org.keycloak.common.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class Base62Test {

    @Test
    public void encode() {
        assertEncode("s");
        assertEncode("sdsfge4rg09u4309tudpoigjsndrgoji3409uer0gr90ued0r90goi");
        assertEncode("sdfoijdsfg9dfo9034dfig sa-d0fsdfj0-j32423");
        assertEncode("sdfoijds£Q()*%$£()*$£)(*FSIHFNSOAIN£*()*£\"()\\$*\")(fg9dfo9034dfig sa-d0fsdfj0-j32423");
    }

    void assertEncode(String original) {
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        String encoded = Base62.encodeToString(bytes);
        byte[] decoded = Base62.decode(encoded);
        assertEquals("Encoded/decoded string does not match", original, new String(decoded, StandardCharsets.UTF_8));
    }

}
