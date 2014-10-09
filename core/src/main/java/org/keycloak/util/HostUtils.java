package org.keycloak.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HostUtils {

    public static String getHostName() {
        String jbossHostName = System.getProperty("jboss.host.name");
        if (jbossHostName != null) {
            return jbossHostName;
        } else {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uhe) {
                throw new IllegalStateException(uhe);
            }
        }
    }

    public static String getIpAddress() {
        try {
            String jbossHostName = System.getProperty("jboss.host.name");
            if (jbossHostName != null) {
                return InetAddress.getByName(jbossHostName).getHostAddress();
            } else {
                return java.net.InetAddress.getLocalHost().getHostAddress();
            }
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
    }
}
