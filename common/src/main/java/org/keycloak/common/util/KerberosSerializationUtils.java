package org.keycloak.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import javax.security.auth.kerberos.KerberosTicket;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.reflections.Reflections;
import sun.security.jgss.GSSCredentialImpl;
import sun.security.jgss.GSSManagerImpl;
import sun.security.jgss.krb5.Krb5InitCredential;
import sun.security.jgss.krb5.Krb5NameElement;
import sun.security.jgss.spi.GSSCredentialSpi;
import sun.security.krb5.Credentials;

/**
 * Provides serialization/deserialization of kerberos {@link org.ietf.jgss.GSSCredential}, so it can be transmitted from auth-server to the application
 * and used for further calls to kerberos-secured services
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosSerializationUtils {

    public static final Oid KRB5_OID;
    public static final Oid KRB5_NAME_OID;
    public static final String JAVA_INFO;

    static {
        try {
            KRB5_OID = new Oid(KerberosConstants.KRB5_OID);
            KRB5_NAME_OID = new Oid(KerberosConstants.KRB5_NAME_OID);
        } catch (GSSException e) {
            throw new RuntimeException(e);
        }

        String javaVersion = System.getProperty("java.version");
        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        String javaVendor = System.getProperty("java.vendor");
        String os = System.getProperty("os.version");
        JAVA_INFO = "Java version: " + javaVersion + ", runtime version: " + javaRuntimeVersion + ", vendor: " + javaVendor + ", os: " + os;
    }

    private KerberosSerializationUtils() {
    }

    public static String serializeCredential(GSSCredential gssCredential) throws KerberosSerializationException {
        try {
            if (gssCredential == null) {
                throw new KerberosSerializationException("Null credential given as input");
            }

            if (!(gssCredential instanceof GSSCredentialImpl)) {
                throw new KerberosSerializationException("Unknown credential type: " + gssCredential.getClass());
            }

            GSSCredentialImpl gssCredImpl = (GSSCredentialImpl) gssCredential;
            Oid[] mechs = gssCredImpl.getMechs();

            for (Oid oid : mechs) {
                if (oid.equals(KRB5_OID)) {
                    int usage = gssCredImpl.getUsage(oid);
                    boolean initiate = (usage == GSSCredential.INITIATE_ONLY || usage == GSSCredential.INITIATE_AND_ACCEPT);

                    GSSCredentialSpi credentialSpi = gssCredImpl.getElement(oid, initiate);
                    if (credentialSpi instanceof Krb5InitCredential) {
                        Krb5InitCredential credential = (Krb5InitCredential) credentialSpi;
                        KerberosTicket kerberosTicket = new KerberosTicket(credential.getEncoded(),
                                credential.getClient(),
                                credential.getServer(),
                                credential.getSessionKey().getEncoded(),
                                credential.getSessionKeyType(),
                                credential.getFlags(),
                                credential.getAuthTime(),
                                credential.getStartTime(),
                                credential.getEndTime(),
                                credential.getRenewTill(),
                                credential.getClientAddresses());
                        return serialize(kerberosTicket);
                    } else {
                        throw new KerberosSerializationException("Unsupported type of credentialSpi: " + credentialSpi.getClass());
                    }
                }
            }

            throw new KerberosSerializationException("Kerberos credential not found. Available mechanisms: " + mechs);
        } catch (IOException e) {
            throw new KerberosSerializationException("Exception occured", e);
        } catch (GSSException e) {
            throw new KerberosSerializationException("Exception occured", e);
        }
    }


    public static GSSCredential deserializeCredential(String serializedCred) throws KerberosSerializationException {
        if (serializedCred == null) {
            throw new KerberosSerializationException("Null credential given as input. Did you enable kerberos credential delegation for your web browser and mapping of gss credential to access token?");
        }

        try {
            Object deserializedCred = deserialize(serializedCred);
            if (!(deserializedCred instanceof KerberosTicket)) {
                throw new KerberosSerializationException("Deserialized object is not KerberosTicket! Type is: " + deserializedCred);
            }

            KerberosTicket ticket = (KerberosTicket) deserializedCred;
            String fullName = ticket.getClient().getName();

            Method getInstance = Reflections.findDeclaredMethod(Krb5NameElement.class, "getInstance", String.class, Oid.class);
            Krb5NameElement krb5Name = Reflections.invokeMethod(true, getInstance, Krb5NameElement.class, null, fullName, KRB5_NAME_OID);

            Credentials krb5CredsInternal = new Credentials(
                    ticket.getEncoded(),
                    ticket.getClient().getName(),
                    ticket.getServer().getName(),
                    ticket.getSessionKey().getEncoded(),
                    ticket.getSessionKeyType(),
                    ticket.getFlags(),
                    ticket.getAuthTime(),
                    ticket.getStartTime(),
                    ticket.getEndTime(),
                    ticket.getRenewTill(),
                    ticket.getClientAddresses()
            );

            Method getInstance2 = Reflections.findDeclaredMethod(Krb5InitCredential.class, "getInstance", Krb5NameElement.class, Credentials.class);
            Krb5InitCredential initCredential = Reflections.invokeMethod(true, getInstance2, Krb5InitCredential.class, null, krb5Name, krb5CredsInternal);

            GSSManagerImpl manager = (GSSManagerImpl) GSSManager.getInstance();
            return new GSSCredentialImpl(manager, initCredential);
        } catch (Exception ioe) {
            throw new KerberosSerializationException("Exception occured", ioe);
        }
    }


    private static String serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            byte[] objBytes = bos.toByteArray();
            return Base64.encodeBytes(objBytes);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static Object deserialize(String serialized) throws ClassNotFoundException, IOException {
        byte[] bytes = Base64.decode(serialized);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return in.readObject();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static class KerberosSerializationException extends RuntimeException {

        public KerberosSerializationException(String message, Throwable cause) {
            super(message + ", " + JAVA_INFO, cause);
        }

        public KerberosSerializationException(String message) {
            super(message + ", " + JAVA_INFO);
        }
    }
}
