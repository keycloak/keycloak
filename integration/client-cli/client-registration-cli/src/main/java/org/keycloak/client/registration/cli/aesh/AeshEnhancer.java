package org.keycloak.client.registration.cli.aesh;

import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Console;

import java.lang.reflect.Field;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AeshEnhancer {

    public static void enhance(AeshConsoleImpl console) {
        try {
            Globals.stdin.setConsole(console);

            Field field = AeshConsoleImpl.class.getDeclaredField("console");
            field.setAccessible(true);
            Console internalConsole = (Console) field.get(console);
            internalConsole.setConsoleCallback(new AeshConsoleCallbackImpl(console));
        } catch (Exception e) {
            throw new RuntimeException("Failed to install Aesh enhancement", e);
        }
    }
}
