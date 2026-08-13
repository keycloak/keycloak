/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.keys;

import java.security.cert.X509Certificate;
import java.util.Date;

import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyWrapper;

import org.jboss.logging.Logger;

/**
 *
 * @author rmartinc
 */
public class KeyNoteUtils {

    private static final Logger logger = Logger.getLogger(KeyNoteUtils.class);

    private KeyNoteUtils() {
    }

    /**
     * Creates two notes in the model to save the key in the cached model. The first
     * note <em>name</em> is the key itself. The second note is the date the
     * certificate expires <em>name.notAfter</em>, if there is a certificate
     * defined in the key (second note can be missing).
     *
     * @param model The model component to attach the notes
     * @param name The name of the note
     * @param key The key to attach
     */
    public static void attachKeyNotes(ComponentModel model, String name, KeyWrapper key) {
        model.setNote(name, key);
        Date notAfter = null;
        if (key.getCertificateChain() != null && !key.getCertificateChain().isEmpty()) {
            notAfter = key.getCertificateChain().stream().map(X509Certificate::getNotAfter).min(Date::compareTo).get();
        }
        if (key.getCertificate() != null) {
            if (notAfter == null) {
                notAfter = key.getCertificate().getNotAfter();
            } else {
                notAfter = notAfter.compareTo(key.getCertificate().getNotAfter()) < 0
                        ? notAfter
                        : key.getCertificate().getNotAfter();
            }
        }
        if (notAfter != null) {
            model.setNote(name + ".notAfter", notAfter);
            if (KeyStatus.ACTIVE.equals(key.getStatus())) {
                checkNotAfter(model, key, notAfter);
            }
        }
    }

    /**
     * Retrieves the key from the note in the model if available. The second key
     * for expiration date is also checked to see if the certificate is expired.
     * If expired the key is transformed into passive.
     *
     * @param model The model with the keys
     * @param name The name of the key
     * @return The attached key or null
     */
    public static KeyWrapper retrieveKeyFromNotes(ComponentModel model, String name) {
        KeyWrapper key = model.getNote(name);
        if (key != null && KeyStatus.ACTIVE.equals(key.getStatus()) && model.hasNote(name + ".notAfter")) {
            Date notAfter = model.getNote(name + ".notAfter");
            checkNotAfter(model, key, notAfter);
        }
        return key;
    }

    private static void checkNotAfter(ComponentModel model, KeyWrapper key, Date notAfter) {
        if (new Date(Time.currentTimeMillis()).compareTo(notAfter) > 0) {
            logger.warnf("Certificate chain for kid '%s' (%s) is not valid anymore, disabling it (certificate expired on %s)",
                    key.getKid(), model.getName(), notAfter);
            key.setStatus(KeyStatus.PASSIVE);
            model.put(Attributes.ACTIVE_KEY, false);
        }
    }
}
