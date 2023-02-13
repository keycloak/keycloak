package org.keycloak.testsuite.runonserver;

import org.keycloak.common.util.Base64;

import java.io.*;

/**
 * Created by st on 26.01.17.
 */
public class SerializationUtil {

    public static String encode(Object function) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(function);
            oos.close();

            return Base64.encodeBytes(os.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object decode(String encoded, ClassLoader classLoader) {
        try {
            byte[] bytes = Base64.decode(encoded);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(is) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass c) throws IOException, ClassNotFoundException {
                    try {
                        return Class.forName(c.getName(), false, classLoader);
                    } catch (ClassNotFoundException e) {
                        throw e;
                    }
                }
            };

            return ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeException(Throwable t) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(t);
            oos.close();

            return "EXCEPTION:" + Base64.encodeBytes(os.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Throwable decodeException(String result) {
        try {
            result = result.substring("EXCEPTION:".length());
            byte[] bytes = Base64.decode(result);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(is);
            return (Throwable) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
