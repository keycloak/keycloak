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

package org.keycloak.storage.ldap.idm.store.ldap;

import org.keycloak.models.ModelException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * <p>Utility class for working with LDAP.</p>
 *
 * @author Pedro Igor
 */
public class LDAPUtil {

    /**
     * <p>Formats the given date.</p>
     *
     * @param date The Date to format.
     *
     * @return A String representing the formatted date.
     */
    public static final String formatDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("You must provide a date.");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss'.0Z'");

        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(date);
    }

    /**
     * <p>
     * Parses dates/time stamps stored in LDAP. Some possible values:
     * </p>
     * <ul>
     *     <li>20020228150820</li>
     *     <li>20030228150820Z</li>
     *     <li>20050228150820.12</li>
     *     <li>20060711011740.0Z</li>
     * </ul>
     *
     * @param date The date string to parse from.
     *
     * @return the Date.
     */
    public static final Date parseDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        try {
            if (date.endsWith("Z")) {
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            } else {
                dateFormat.setTimeZone(TimeZone.getDefault());
            }

            return dateFormat.parse(date);
        } catch (Exception e) {
            throw new ModelException("Error converting ldap date.", e);
        }
    }



    /**
     * <p>Creates a byte-based {@link String} representation of a raw byte array representing the value of the
     * <code>objectGUID</code> attribute retrieved from Active Directory.</p>
     *
     * <p>The returned string is useful to perform queries on AD based on the <code>objectGUID</code> value. Eg.:</p>
     *
     * <p>
     * String filter = "(&(objectClass=*)(objectGUID" + EQUAL + convertObjectGUIDToByteString(objectGUID) + "))";
     * </p>
     *
     * @param objectGUID A raw byte array representing the value of the <code>objectGUID</code> attribute retrieved from
     * Active Directory.
     *
     * @return A byte-based String representation in the form of \[0]\[1]\[2]\[3]\[4]\[5]\[6]\[7]\[8]\[9]\[10]\[11]\[12]\[13]\[14]\[15]
     */
    public static String convertObjectGUIDToByteString(byte[] objectGUID) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < objectGUID.length; i++) {
            String transformed = prefixZeros((int) objectGUID[i] & 0xFF);
            result.append("\\");
            result.append(transformed);
        }

        return result.toString();
    }

    /**
     * <p>Decode a raw byte array representing the value of the <code>objectGUID</code> attribute retrieved from Active
     * Directory.</p>
     *
     * <p>The returned string is useful to directly bind an entry. Eg.:</p>
     *
     * <p>
     * String bindingString = decodeObjectGUID(objectGUID);
     * <br/>
     * Attributes attributes = ctx.getAttributes(bindingString);
     * </p>
     *
     * @param objectGUID A raw byte array representing the value of the <code>objectGUID</code> attribute retrieved from
     * Active Directory.
     *
     * @return A string representing the decoded value in the form of [3][2][1][0]-[5][4]-[7][6]-[8][9]-[10][11][12][13][14][15].
     */
    public static String decodeObjectGUID(byte[] objectGUID) {
        StringBuilder displayStr = new StringBuilder();

        displayStr.append(convertToDashedString(objectGUID));

        return displayStr.toString();
    }

    /**
     * <p>Decode a raw byte array representing the value of the <code>guid</code> attribute retrieved from Novell
     * eDirectory.</p>
     *
     * @param guid A raw byte array representing the value of the <code>guid</code> attribute retrieved from
     * Novell eDirectory.
     *
     * @return A string representing the decoded value in the form of [0][1][2][3]-[4][5]-[6][7]-[8][9]-[10][11][12][13][14][15].
     */
    public static String decodeGuid(byte[] guid) {
        byte[] withBigEndian = new byte[] { guid[3], guid[2], guid[1], guid[0],
            guid[5], guid[4],
            guid[7], guid[6],
            guid[8], guid[9], guid[10], guid[11], guid[12], guid[13], guid[14], guid[15]
        };
        return convertToDashedString(withBigEndian);
    }

    private static String convertToDashedString(byte[] objectGUID) {
        StringBuilder displayStr = new StringBuilder();

        displayStr.append(prefixZeros((int) objectGUID[3] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[2] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[1] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[0] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[5] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[4] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[7] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[6] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[8] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[9] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[10] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[11] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[12] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[13] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[14] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[15] & 0xFF));

        return displayStr.toString();
    }

    private static String prefixZeros(int value) {
        if (value <= 0xF) {
            StringBuilder sb = new StringBuilder("0");
            sb.append(Integer.toHexString(value));
            return sb.toString();
        } else {
            return Integer.toHexString(value);
        }
    }


}
