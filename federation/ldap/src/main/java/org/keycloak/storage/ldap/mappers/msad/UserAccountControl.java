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

package org.keycloak.storage.ldap.mappers.msad;

/**
 * See https://support.microsoft.com/en-us/kb/305144
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAccountControl {

    public static final long SCRIPT = 0x0001L;
    public static final long ACCOUNTDISABLE = 0x0002L;
    public static final long HOMEDIR_REQUIRED = 0x0008L;
    public static final long LOCKOUT = 0x0010L;
    public static final long PASSWD_NOTREQD = 0x0020L;
    public static final long PASSWD_CANT_CHANGE = 0x0040L;
    public static final long ENCRYPTED_TEXT_PWD_ALLOWED = 0x0080L;
    public static final long TEMP_DUPLICATE_ACCOUNT = 0x0100L;
    public static final long NORMAL_ACCOUNT = 0x0200L;
    public static final long INTERDOMAIN_TRUST_ACCOUNT = 0x0800L;
    public static final long WORKSTATION_TRUST_ACCOUNT = 0x1000L;
    public static final long SERVER_TRUST_ACCOUNT = 0x2000L;
    public static final long DONT_EXPIRE_PASSWORD = 0x10000L;
    public static final long MNS_LOGON_ACCOUNT = 0x20000L;
    public static final long SMARTCARD_REQUIRED = 0x40000L;
    public static final long TRUSTED_FOR_DELEGATION = 0x80000L;
    public static final long NOT_DELEGATED = 0x100000L;
    public static final long USE_DES_KEY_ONLY = 0x200000L;
    public static final long DONT_REQ_PREAUTH = 0x400000L;
    public static final long PASSWORD_EXPIRED = 0x800000L;
    public static final long TRUSTED_TO_AUTH_FOR_DELEGATION = 0x1000000L;
    public static final long PARTIAL_SECRETS_ACCOUNT = 0x04000000L;

    private long value;

    public UserAccountControl(long value) {
        this.value = value;
    }

    public boolean has(long feature) {
        return (this.value & feature) > 0;
    }

    public void add(long feature) {
        if (!has(feature)) {
            this.value += feature;
        }
    }

    public void remove(long feature) {
        if (has(feature)) {
            this.value -= feature;
        }
    }

    public long getValue() {
        return value;
    }
}
