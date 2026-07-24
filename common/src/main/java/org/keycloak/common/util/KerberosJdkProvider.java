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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.keycloak.common.constants.KerberosConstants;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.jboss.logging.Logger;

/**
 * Provides abstraction to handle differences between various JDK vendors (Sun, IBM)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class KerberosJdkProvider {

    private static final Logger logger = Logger.getLogger(KerberosJdkProvider.class);

    public abstract Configuration createJaasConfigurationForServer(String keytab, String serverPrincipal, boolean debug);
    public abstract Configuration createJaasConfigurationForUsernamePasswordLogin(boolean debug);

    public abstract KerberosTicket gssCredentialToKerberosTicket(KerberosTicket kerberosTicket, GSSCredential gssCredential);



    public GSSCredential kerberosTicketToGSSCredential(KerberosTicket kerberosTicket) {
        return kerberosTicketToGSSCredential(kerberosTicket, GSSCredential.DEFAULT_LIFETIME, GSSCredential.INITIATE_ONLY);
    }

    /**
     * @return true if Kerberos (GSS API) is available in underlying JDK and it is possible to use it. False otherwise
     */
    public boolean isKerberosAvailable() {
        GSSManager gssManager = GSSManager.getInstance();
        List<Oid> supportedMechs = Arrays.asList(gssManager.getMechs());
        if (supportedMechs.contains(KerberosConstants.KRB5_OID)) {
            return true;
        } else {
            logger.warnf("Kerberos feature not supported by JDK. Check security providers for your JDK in java.security. Supported mechanisms: %s", supportedMechs);
            return false;
        }
    }

    // Actually can use same on both JDKs
    public GSSCredential kerberosTicketToGSSCredential(KerberosTicket kerberosTicket, final int lifetime, final int usage) {
        try {
            final GSSManager gssManager = GSSManager.getInstance();

            KerberosPrincipal kerberosPrincipal = kerberosTicket.getClient();
            String krbPrincipalName = kerberosTicket.getClient().getName();
            final GSSName gssName = gssManager.createName(krbPrincipalName, KerberosConstants.KRB5_NAME_OID);

            Set<KerberosPrincipal> principals = Collections.singleton(kerberosPrincipal);
            Set<GSSName> publicCreds = Collections.singleton(gssName);
            Set<KerberosTicket> privateCreds = Collections.singleton(kerberosTicket);
            Subject subject = new Subject(false, principals, publicCreds, privateCreds);

            return Subject.doAs(subject, new PrivilegedExceptionAction<GSSCredential>() {

                @Override
                public GSSCredential run() throws Exception {
                    return gssManager.createCredential(gssName, lifetime, KerberosConstants.KRB5_OID, usage);
                }

            });
        } catch (Exception e) {
            throw new KerberosSerializationUtils.KerberosSerializationException("Unexpected exception during convert KerberosTicket to GSSCredential", e);
        }
    }


    public static KerberosJdkProvider getProvider() {
        if (Environment.IS_IBM_JAVA) {
            return new IBMJDKProvider();
        } else {
            return new SunJDKProvider();
        }
    }


    // IMPL Subclasses


    // Works for Oracle and OpenJDK
    private static class SunJDKProvider extends KerberosJdkProvider {


        @Override
        public Configuration createJaasConfigurationForServer(final String keytab, final String serverPrincipal, final boolean debug) {
            return new Configuration() {

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    Map<String, Object> options = new HashMap<>();
                    options.put("storeKey", "true");
                    options.put("doNotPrompt", "true");
                    options.put("isInitiator", "false");
                    options.put("useKeyTab", "true");

                    options.put("keyTab", keytab);
                    options.put("principal", serverPrincipal);
                    options.put("debug", String.valueOf(debug));
                    AppConfigurationEntry kerberosLMConfiguration = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                    return new AppConfigurationEntry[] { kerberosLMConfiguration };
                }
            };
        }


        @Override
        public Configuration createJaasConfigurationForUsernamePasswordLogin(final boolean debug) {
            return new Configuration() {

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    Map<String, Object> options = new HashMap<>();
                    options.put("storeKey", "true");
                    options.put("debug", String.valueOf(debug));
                    AppConfigurationEntry kerberosLMConfiguration = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                    return new AppConfigurationEntry[] { kerberosLMConfiguration };
                }
            };
        }


        // Note: input kerberosTicket is null for Sun based JDKs
        @Override
        public KerberosTicket gssCredentialToKerberosTicket(KerberosTicket kerberosTicket, GSSCredential gssCredential) {
            try {
                Class<?> gssUtil = Class.forName("com.sun.security.jgss.GSSUtil");
                Method createSubject = gssUtil.getMethod("createSubject", GSSName.class, GSSCredential.class);
                Subject subject = (Subject) createSubject.invoke(null, null, gssCredential);
                Set<KerberosTicket> kerberosTickets = subject.getPrivateCredentials(KerberosTicket.class);
                Iterator<KerberosTicket> iterator = kerberosTickets.iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                } else {
                    throw new KerberosSerializationUtils.KerberosSerializationException("Not available kerberosTicket in subject credentials. Subject was: " + subject.toString());
                }
            } catch (KerberosSerializationUtils.KerberosSerializationException ke) {
                throw ke;
            } catch (Exception e) {
                throw new KerberosSerializationUtils.KerberosSerializationException("Unexpected error during convert GSSCredential to KerberosTicket", e);
            }
        }

    }


    // Works for IBM JDK
    private static class IBMJDKProvider extends KerberosJdkProvider {

        @Override
        public Configuration createJaasConfigurationForServer(String keytab, final String serverPrincipal, final boolean debug) {
            final String keytabUrl = getKeytabURL(keytab);

            return new Configuration() {

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    Map<String, Object> options = new HashMap<>();
                    options.put("noAddress", "true");
                    options.put("credsType","acceptor");
                    options.put("useKeytab", keytabUrl);
                    options.put("principal", serverPrincipal);
                    options.put("debug", String.valueOf(debug));

                    AppConfigurationEntry kerberosLMConfiguration = new AppConfigurationEntry("com.ibm.security.auth.module.Krb5LoginModule", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                    return new AppConfigurationEntry[] { kerberosLMConfiguration };
                }
            };
        }

        private String getKeytabURL(String keytab) {
            try {
                return new File(keytab).toURI().toURL().toString();
            } catch (MalformedURLException mfe) {
                System.err.println("Invalid keytab location specified in configuration: " + keytab);
                mfe.printStackTrace();
                return keytab;
            }
        }


        @Override
        public Configuration createJaasConfigurationForUsernamePasswordLogin(final boolean debug) {
            return new Configuration() {

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    Map<String, Object> options = new HashMap<>();
                    options.put("credsType","initiator");
                    options.put("noAddress", "true");
                    options.put("debug", String.valueOf(debug));
                    AppConfigurationEntry kerberosLMConfiguration = new AppConfigurationEntry("com.ibm.security.auth.module.Krb5LoginModule", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                    return new AppConfigurationEntry[] { kerberosLMConfiguration };
                }
            };
        }


        // For IBM, kerberosTicket was set on JAAS Subject, so we can just return it
        @Override
        public KerberosTicket gssCredentialToKerberosTicket(KerberosTicket kerberosTicket, GSSCredential gssCredential) {
            if (kerberosTicket == null) {
                throw new KerberosSerializationUtils.KerberosSerializationException("Not available kerberosTicket in subject credentials in IBM JDK");
            } else {
                return kerberosTicket;
            }
        }
    }
}
