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

package org.keycloak.common.util;

import org.ietf.jgss.GSSCredential;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;

/**
 * Provides serialization/deserialization of kerberos {@link org.ietf.jgss.GSSCredential}, so it can be transmitted from auth-server to the application
 * and used for further calls to kerberos-secured services
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosSerializationUtils {

    public static final String JAVA_INFO;

    static {
        String javaVersion = System.getProperty("java.version");
        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        String javaVendor = System.getProperty("java.vendor");
        String os = System.getProperty("os.version");
        JAVA_INFO = "Java version: " + javaVersion + ", runtime version: " + javaRuntimeVersion + ", vendor: " + javaVendor + ", os: " + os;
    }

    private KerberosSerializationUtils() {
    }

    public static String serializeCredential(KerberosTicket kerberosTicket, GSSCredential gssCredential) throws KerberosSerializationException {
        try {
            if (gssCredential == null) {
                throw new KerberosSerializationException("Null credential given as input");
            }

            kerberosTicket = KerberosJdkProvider.getProvider().gssCredentialToKerberosTicket(kerberosTicket, gssCredential);

            return serialize(kerberosTicket);
        } catch (IOException e) {
            throw new KerberosSerializationException("Unexpected exception when serialize GSSCredential", e);
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

            return KerberosJdkProvider.getProvider().kerberosTicketToGSSCredential(ticket);
        } catch (KerberosSerializationException ke) {
            throw ke;
        } catch (Exception ioe) {
            throw new KerberosSerializationException("Unexpected exception when deserialize GSSCredential", ioe);
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
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(bis);
            DelegatingSerializationFilter.builder()
                    .addAllowedClass(KerberosTicket.class)
                    .addAllowedClass(KerberosPrincipal.class)
                    .addAllowedClass(InetAddress.class)
                    .addAllowedPattern("javax.security.auth.kerberos.KeyImpl")
                    .setFilter(in);
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
