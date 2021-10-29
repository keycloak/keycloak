package org.keycloak.quarkus.runtime.cli.command;

import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {
    @Override
    public String[] getVersion() {
        return new String[]{"Keycloak ${sys:kc.version}",
                            "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                            "OS: ${os.name} ${os.version} ${os.arch}%n"
                            };
}
}
