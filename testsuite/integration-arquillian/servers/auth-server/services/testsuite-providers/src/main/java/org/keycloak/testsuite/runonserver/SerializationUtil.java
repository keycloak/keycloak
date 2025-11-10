package org.keycloak.testsuite.runonserver;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Base64;

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

            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object decode(String encoded, ClassLoader classLoader) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
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

            return "EXCEPTION:" + Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (NotSerializableException e) {
            // when the exception can't be serialized, at least log the original exception, so it can be analyzed
            throw new RuntimeException("Unable to serialize exception due to not serializable class " + e.getMessage(), t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Throwable decodeException(String result) {
        try {
            result = result.substring("EXCEPTION:".length());
            byte[] bytes = Base64.getDecoder().decode(result);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(is);
            return (Throwable) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
