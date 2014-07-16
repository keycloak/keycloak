package org.keycloak.admin.client.utils;

import org.keycloak.admin.client.KeycloakException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public final class StringUtils {

    private StringUtils() {}

    public static String inputStreamToString(InputStream is){
        try{
            Scanner s = new Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }finally{
            try {
                if(is != null)
                    is.close();
            } catch (IOException e) {
                throw new KeycloakException(e);
            }
        }
    }

}
