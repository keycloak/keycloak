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

package org.keycloak.util.ldap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.crypto.EncryptionHandler;
import org.apache.kerby.kerberos.kerb.keytab.Keytab;
import org.apache.kerby.kerberos.kerb.keytab.KeytabEntry;
import org.apache.kerby.kerberos.kerb.type.KerberosTime;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionKey;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionType;
import org.apache.kerby.kerberos.kerb.type.base.PrincipalName;

/**
 * Helper utility for creating Keytab files.
 *
 * @author Josef Cacek
 */
public class KerberosKeytabCreator {

    // Same 5 types that ApacheDS KeyDerivationInterceptor derives when loading LDIF entries;
    // the old KerberosKeyFactory.getKerberosKeys() produced these implicitly.
    private static final EncryptionType[] KEYTAB_ENC_TYPES = {
            EncryptionType.AES256_CTS_HMAC_SHA1_96,
            EncryptionType.AES128_CTS_HMAC_SHA1_96,
            EncryptionType.DES3_CBC_SHA1_KD,
            EncryptionType.ARCFOUR_HMAC,
            EncryptionType.DES_CBC_MD5
    };

    // Public methods --------------------------------------------------------

    /**
     * The main.
     *
     * @param args
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        if (args == null || args.length != 3) {
            System.out.println("Kerberos keytab generator");
            System.out.println("-------------------------");
            System.out.println("Arguments missing or invalid. Required arguments are: <principalName> <passPhrase> <outputKeytabFile>");
            System.out.println("Example of usage:");
            System.out.println("java -jar embedded-ldap/target/embedded-ldap.jar keytabCreator HTTP/localhost@KEYCLOAK.ORG httppassword http.keytab");
        } else {
            final File keytabFile = new File(args[2]);
            createKeytab(args[0], args[1], keytabFile);
            System.out.println("Keytab file was created: " + keytabFile.getAbsolutePath() + ", principal: " + args[0] + ", passphrase: " + args[1]);
        }
    }

    // Just for the reflection purposes
    public static void execute(String[] args, Properties defaultProperties) throws Exception {
        main(args);
    }

    /**
     * Creates a keytab file for given principal.
     *
     * @param principalName
     * @param passPhrase
     * @param keytabFile
     * @throws IOException
     */
    public static void createKeytab(final String principalName, final String passPhrase, final File keytabFile)
            throws IOException {
        final KerberosTime timeStamp = KerberosTime.now();
        final PrincipalName principal = new PrincipalName(principalName);

        final Keytab keytab = new Keytab();
        final List<KeytabEntry> entries = new ArrayList<>();
        try {
            for (EncryptionType encType : KEYTAB_ENC_TYPES) {
                EncryptionKey key = EncryptionHandler.string2Key(principalName, passPhrase, encType);
                System.out.println("Adding keytab entry of type: " + encType.getName());
                entries.add(new KeytabEntry(principal, timeStamp, key.getKvno(), key));
            }
        } catch (KrbException e) {
            throw new IOException("Failed to create keytab entries", e);
        }
        keytab.addKeytabEntries(entries);
        keytab.store(keytabFile);
    }
}
