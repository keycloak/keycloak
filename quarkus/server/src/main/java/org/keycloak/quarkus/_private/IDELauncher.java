package org.keycloak.quarkus._private;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.runtime.Quarkus;

/**
 * <p>This main class should be used to start the server in dev mode for development purposes. By running this class,
 * developers should be able to mimic any server behavior and configuration as if they were using the CLI.
 *
 * <p>There are some limitations during development such as:
 *
 * <ul>
 *     <li>Transient dependencies from the keycloak server extension (runtime module) are not eligible for hot-reload</li>
 *     <li>Code changes such as changing the structure of classes (e.g.: new/change methods) should still require a JVM restart</li>
 * </ul>
 *
 * <p>Despite the limitations, it should be possible to debug the extension (e.g.: deployment steps) as well as perform changes at runtime
 * without having to restart the JVM.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class IDELauncher {

    public static void main(String[] args) {
        List<String> devArgs = new ArrayList<>();

        devArgs.addAll(Arrays.asList(args));

        if (devArgs.isEmpty()) {
            devArgs.add("start-dev");
        }

        Quarkus.run(devArgs.toArray(new String[devArgs.size()]));
    }
}
