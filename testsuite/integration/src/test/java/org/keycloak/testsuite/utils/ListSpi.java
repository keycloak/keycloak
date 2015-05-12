package org.keycloak.testsuite.utils;

import org.keycloak.provider.Spi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ListSpi {

    public static void main(String[] args) {
        List<String> l = new LinkedList<>();
        for (Spi s : ServiceLoader.load(Spi.class)) {
            l.add(fixedLength(s.getName()) + s.isPrivate());
        }
        Collections.sort(l);
        System.out.println(fixedLength("SPI") + "Private");
        System.out.println("-------------------------------------");
        for (String s : l) {
            System.out.println(s);
        }
    }

    public static String fixedLength(String s) {
        while (s.length() < 30) {
            s = s + " ";
        }
        return s;
    }
}
