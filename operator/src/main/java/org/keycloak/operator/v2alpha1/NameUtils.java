package org.keycloak.operator.v2alpha1;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class NameUtils {

    // DNS Subdomain Names
    // https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#dns-subdomain-names
    // TODO: test me
    public static String getValidDNSSubdomainName(String in) {
        // contain only lowercase alphanumeric characters, '-' or '.'
        var out = in.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        var sb = new StringBuilder();
        // contain no more than 253 characters
        var max = Math.min(254, out.length);

        for (var i = 0; i < max; i++) {
            var b = (char)out[i];

            if (i == 0 && !Character.isAlphabetic(b)) {
                // start with an alphanumeric character
                sb.append('a');
                if (max == 254) {
                    max--;
                }
                sb.append(b);
            } else if (i == (max - 1) && !Character.isAlphabetic(b)) {
                // end with an alphanumeric character
                sb.append('z');
            } else {
                // contain only lowercase alphanumeric characters, '-' or '.'
                if (Character.isAlphabetic(b) ||
                    Character.isDigit(b) ||
                    b == '-' ||
                    b == '.'
                ) {
                    sb.append(b);
                }
            }
        }

        return sb.toString();
    }

}
