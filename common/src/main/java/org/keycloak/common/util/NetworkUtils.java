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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Locale;

/**
 * Utility methods related to networking.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NetworkUtils {

    private static final int MAX_GROUP_LENGTH = 4;
    private static final int IPV6_LEN = 8;
    private static final boolean can_bind_to_mcast_addr; // are we running on Linux ?

    static {
        can_bind_to_mcast_addr = checkForLinux() || checkForSolaris() || checkForHp();
    }

    public static String formatPossibleIpv6Address(String address) {
        if(address == null) {
            return null;
        }
        String ipv6Address;
        if (address.startsWith("[") && address.endsWith("]")) {
            ipv6Address = address.substring(0, address.lastIndexOf(']')).substring(1);
        } else {
            ipv6Address = address;
        }
        // Definitely not an IPv6, return untouched input.
        if (!mayBeIPv6Address(ipv6Address)) {
            return ipv6Address;
        }
        return '[' + canonize(ipv6Address) + ']';
    }

    /**
     * <p>Convert IPv6 address into RFC 5952 form.
     * E.g. 2001:db8:0:1:0:0:0:1 -> 2001:db8:0:1::1</p>
     *
     * <p>Method is null safe, and if IPv4 address or host name is passed to the
     * method it is returned without any processing.</p>
     *
     * <p>Method also supports IPv4 in IPv6 (e.g. 0:0:0:0:0:ffff:192.0.2.1 ->
     * ::ffff:192.0.2.1), and zone ID (e.g. fe80:0:0:0:f0f0:c0c0:1919:1234%4
     * -> fe80::f0f0:c0c0:1919:1234%4).</p>
     *
     * @param ipv6Address String representing valid IPv6 address.
     * @return String representing IPv6 in canonical form.
     * @throws IllegalArgumentException if IPv6 format is unacceptable.
     */
    public static String canonize(String ipv6Address) throws IllegalArgumentException {

        if (ipv6Address == null) {
            return null;
        }

        // Definitely not an IPv6, return untouched input.
        if (!mayBeIPv6Address(ipv6Address)) {
            return ipv6Address;
        }

        // Length without zone ID (%zone) or IPv4 address
        int ipv6AddressLength = ipv6Address.length();
        if (isIPv4AddressInIPv6(ipv6Address)) {
            // IPv4 in IPv6
            // e.g. 0:0:0:0:0:FFFF:127.0.0.1
            int lastColonPos = ipv6Address.lastIndexOf(":");
            int lastColonsPos = ipv6Address.lastIndexOf("::");
            if (lastColonsPos >= 0 && lastColonPos == lastColonsPos + 1) {
                // IPv6 part ends with two consecutive colons, last colon is part of IPv6 format.
                // e.g. ::127.0.0.1
                ipv6AddressLength = lastColonPos + 1;
            } else {
                // IPv6 part ends with only one colon, last colon is not part of IPv6 format.
                // e.g. ::FFFF:127.0.0.1
                ipv6AddressLength = lastColonPos;
            }
        } else if (ipv6Address.contains(":") && ipv6Address.contains("%")) {
            // Zone ID
            // e.g. fe80:0:0:0:f0f0:c0c0:1919:1234%4
            ipv6AddressLength = ipv6Address.lastIndexOf("%");
        }

        StringBuilder result = new StringBuilder();
        char [][] groups = new char[IPV6_LEN][MAX_GROUP_LENGTH];
        int groupCounter = 0;
        int charInGroupCounter = 0;

        // Index of the current zeroGroup, -1 means not found.
        int zeroGroupIndex = -1;
        int zeroGroupLength = 0;

        // maximum length zero group, if there is more then one, then first one
        int maxZeroGroupIndex = -1;
        int maxZeroGroupLength = 0;

        boolean isZero = true;
        boolean groupStart = true;

        /*
         *  Two consecutive colons, initial expansion.
         *  e.g. 2001:db8:0:0:1::1 -> 2001:db8:0:0:1:0:0:1
         */
        StringBuilder expanded = new StringBuilder(ipv6Address);
        int colonsPos = ipv6Address.indexOf("::");
        int length = ipv6AddressLength;
        int change = 0;

        if (colonsPos >= 0 && colonsPos < ipv6AddressLength - 2) {
            int colonCounter = 0;
            for (int i = 0; i < ipv6AddressLength; i++) {
                if (ipv6Address.charAt(i) == ':') {
                    colonCounter++;
                }
            }

            if (colonsPos == 0) {
                expanded.insert(0, "0");
                change = change + 1;
            }

            for (int i = 0; i < IPV6_LEN - colonCounter; i++) {
                expanded.insert(colonsPos + 1, "0:");
                change = change + 2;
            }


            if (colonsPos == ipv6AddressLength - 2) {
                expanded.setCharAt(colonsPos + change + 1, '0');
            } else {
                expanded.deleteCharAt(colonsPos + change + 1);
                change = change - 1;
            }
            length = length + change;
        }


        // Processing one char at the time
        for (int charCounter = 0; charCounter < length; charCounter++) {
            char c = expanded.charAt(charCounter);
            if (c >= 'A' && c <= 'F') {
                c = (char) (c + 32);
            }
            if (c != ':') {
                groups[groupCounter][charInGroupCounter] = c;
                if (!(groupStart && c == '0')) {
                    ++charInGroupCounter;
                    groupStart = false;
                }
                if (c != '0') {
                    isZero = false;
                }
            }
            if (c == ':' || charCounter == (length - 1)) {
                // We reached end of current group
                if (isZero) {
                    ++zeroGroupLength;
                    if (zeroGroupIndex == -1) {
                        zeroGroupIndex = groupCounter;
                    }
                }

                if (!isZero || charCounter == (length - 1)) {
                    // We reached end of zero group
                    if (zeroGroupLength > maxZeroGroupLength) {
                        maxZeroGroupLength = zeroGroupLength;
                        maxZeroGroupIndex = zeroGroupIndex;
                    }
                    zeroGroupLength = 0;
                    zeroGroupIndex = -1;
                }
                ++groupCounter;
                charInGroupCounter = 0;
                isZero = true;
                groupStart = true;
            }
        }

        int numberOfGroups = groupCounter;

        // Output results
        for (groupCounter = 0; groupCounter < numberOfGroups; groupCounter++) {
            if (maxZeroGroupLength <= 1 || groupCounter < maxZeroGroupIndex
                    || groupCounter >= maxZeroGroupIndex + maxZeroGroupLength) {
                for (int j = 0; j < MAX_GROUP_LENGTH; j++) {
                    if (groups[groupCounter][j] != 0) {
                        result.append(groups[groupCounter][j]);
                    }
                }
                if (groupCounter < (numberOfGroups - 1)
                        && (groupCounter != maxZeroGroupIndex - 1
                        || maxZeroGroupLength <= 1)) {
                    result.append(':');
                }
            } else if (groupCounter == maxZeroGroupIndex) {
                result.append("::");
            }
        }

        // Solve problem with three colons in IPv4 in IPv6 format
        // e.g. 0:0:0:0:0:0:127.0.0.1 -> :::127.0.0.1 -> ::127.0.0.1
        int resultLength = result.length();
        if (result.charAt(resultLength - 1) == ':' && ipv6AddressLength < ipv6Address.length()
                && ipv6Address.charAt(ipv6AddressLength) == ':') {
            result.delete(resultLength - 1, resultLength);
        }

        /*
         * Append IPv4 from IPv4-in-IPv6 format or Zone ID
         */
        for (int i = ipv6AddressLength; i < ipv6Address.length(); i++) {
            result.append(ipv6Address.charAt(i));
        }

        return result.toString();
    }

    /**
     * Heuristic check if string might be an IPv6 address.
     *
     * @param input Any string or null
     * @return true, if input string contains only hex digits and at least two colons, before '.' or '%' character.
     */
    private static boolean mayBeIPv6Address(String input) {
        if (input == null) {
            return false;
        }

        boolean result = false;
        int colonsCounter = 0;
        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (c == '.' || c == '%') {
                // IPv4 in IPv6 or Zone ID detected, end of checking.
                break;
            }
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F') || c == ':')) {
                return false;
            } else if (c == ':') {
                colonsCounter++;
            }
        }
        if (colonsCounter >= 2) {
            result = true;
        }
        return result;
    }
    /**
     * Check if it is an IPv4 in IPv6 format.
     * e.g. 0:0:0:0:0:FFFF:127.0.0.1
     *
     * @param ipv6Address the address
     * @return true, if input string is an IPv4 address in IPv6 format.
     */
    private static boolean isIPv4AddressInIPv6(String ipv6Address) {
        return (ipv6Address.contains(":") && ipv6Address.contains("."));
    }

    /**
     * Formats input address. For IPV4 returns simply host address, for IPV6 formats address according to <a
     * href="http://tools.ietf.org/html/rfc5952">RFC5952</a> rules. It does not embed IPV6 address in '[', ']', since those are part of IPV6 URI literal.
     *
     * @param inet
     * @return
     */
    public static String formatAddress(InetAddress inet){
        if(inet == null){
            throw new NullPointerException();
        }
        if(inet instanceof Inet4Address){
            return inet.getHostAddress();
        } else if (inet instanceof Inet6Address){
            byte[] byteRepresentation = inet.getAddress();
            int[] hexRepresentation = new int[IPV6_LEN];

            for(int i=0;i < hexRepresentation.length;i++){
                hexRepresentation[i] = ( byteRepresentation[2*i] & 0xFF) << 8 | ( byteRepresentation[2*i+1] & 0xFF );
            }
            compactLongestZeroSequence(hexRepresentation);
            return formatAddress6(hexRepresentation);
        } else {
            return inet.getHostAddress();
        }
    }

    /**
     * Converts socket address into string literal, which has form: 'address:port'. Example:<br>
     * <ul>
     *      <li>127.0.0.1:8080</li>
     *      <li>dns.name.com:8080</li>
     *      <li>[0fe:1::20]:8080</li>
     *      <li>[::1]:8080</li>
     * </ul>
     * @param inet
     * @return
     */
    public static String formatAddress(InetSocketAddress inet){
        if(inet == null){
            throw new NullPointerException();
        }
        StringBuilder result = new StringBuilder();
        if(inet.isUnresolved()){
            result.append(inet.getHostName());
        }else{
            result.append(formatPossibleIpv6Address(formatAddress(inet.getAddress())));
        }
        result.append(":").append(inet.getPort());
        return result.toString();
    }

    /**
     * Converts IPV6 int[] representation into valid IPV6 string literal. Sequence of '-1' values are converted into '::'.
     * @param hexRepresentation
     * @return
     */
    private static String formatAddress6(int[] hexRepresentation){
        if(hexRepresentation == null){
            throw new NullPointerException();
        }
        if(hexRepresentation.length != IPV6_LEN){
            throw new IllegalArgumentException();
        }
        StringBuilder stringBuilder = new StringBuilder();
        boolean inCompressedSection = false;
        for(int i = 0;i<hexRepresentation.length;i++){
            if(hexRepresentation[i] == -1){
                if(!inCompressedSection){
                    inCompressedSection = true;
                    if(i == 0){
                        stringBuilder.append("::");
                    } else {
                        stringBuilder.append(':');
                    }
                }
            } else {
                inCompressedSection = false;
                stringBuilder.append(Integer.toHexString(hexRepresentation[i]));
                if(i+1<hexRepresentation.length){
                    stringBuilder.append(":");
                }
            }
        }
        return stringBuilder.toString();
    }

    public static boolean isBindingToMulticastDressSupported() {
        return can_bind_to_mcast_addr;
    }

    private static void compactLongestZeroSequence(int[] hexRepresentatoin){
        int bestRunStart = -1;
        int bestRunLen = -1;
        boolean inRun = false;
        int runStart = -1;
        for(int i=0;i<hexRepresentatoin.length;i++){

            if(hexRepresentatoin[i] == 0){
                if(!inRun){
                    runStart = i;
                    inRun = true;
                }
            } else {
                if(inRun){
                    inRun = false;
                    int runLen = i - runStart;
                    if(bestRunLen < 0){
                        bestRunStart = runStart;
                        bestRunLen = runLen;
                    } else {
                        if(runLen > bestRunLen){
                            bestRunStart = runStart;
                            bestRunLen = runLen;
                        }
                    }
                }
            }
        }
        if(bestRunStart >=0){
            Arrays.fill(hexRepresentatoin, bestRunStart, bestRunStart + bestRunLen, -1);
        }
    }

    private static boolean checkForLinux() {
        return checkForPresence("os.name", "linux");
    }

    private static boolean checkForHp() {
        return checkForPresence("os.name", "hp");
    }

    private static boolean checkForSolaris() {
        return checkForPresence("os.name", "sun");
    }

    public static boolean checkForWindows() {
        return checkForPresence("os.name", "win");
    }

    public static boolean checkForMac() {
        return checkForPresence("os.name", "mac");
    }

    private static boolean checkForPresence(final String key, final String value) {
        final String tmp = System.getProperty(key, value);
        try {
            return tmp != null && tmp.trim().toLowerCase(Locale.ENGLISH).startsWith(value);
        } catch (Throwable t) {
            return false;
        }
    }

    // No instantiation
    private NetworkUtils() {

    }
}
