package org.keycloak.util;


import net.iharder.Base64;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Base64Url {
    public static String encode(byte[] bytes) {
        String s = Base64.encodeBytes(bytes);
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    public static byte[] decode(String s) {
        s = s.replace('-', '+'); // 62nd char of encoding
        s = s.replace('_', '/'); // 63rd char of encoding
        switch (s.length() % 4) // Pad with trailing '='s
        {
            case 0:
                break; // No pad chars in this case
            case 2:
                s += "==";
                break; // Two pad chars
            case 3:
                s += "=";
                break; // One pad char
            default:
                throw new RuntimeException(
                        "Illegal base64url string!");
        }
        try {
            return Base64.decode(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
