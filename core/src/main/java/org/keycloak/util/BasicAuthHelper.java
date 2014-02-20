package org.keycloak.util;

import net.iharder.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BasicAuthHelper
{
    public static String createHeader(String username, String password)
    {
        StringBuffer buf = new StringBuffer(username);
        buf.append(':').append(password);
        try
        {
            return "Basic " + Base64.encodeBytes(buf.toString().getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String[] parseHeader(String header)
    {
        if (header.length() < 6) return null;
        String type = header.substring(0, 5);
        type = type.toLowerCase();
        if (!type.equalsIgnoreCase("Basic")) return null;
        String val = header.substring(6);
        try {
            val = new String(Base64.decode(val.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] split = val.split(":");
        if (split.length != 2) return null;
        return split;
    }
}
