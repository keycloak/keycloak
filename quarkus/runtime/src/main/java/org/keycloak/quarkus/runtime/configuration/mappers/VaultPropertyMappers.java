package org.keycloak.quarkus.runtime.configuration.mappers;

final class VaultPropertyMappers {

    private VaultPropertyMappers() {
    }

    public static PropertyMapper[] getVaultPropertyMappers() {
        return new PropertyMapper[] {
                builder()
                        .from("vault")
                        .description("Enables a vault provider.")
                        .expectedValues("file", "hashicorp")
                        .paramLabel("provider")
                        .isBuildTimeProperty(true)
                        .build(),
                builder()
                        .from("vault-dir")
                        .to("kc.spi-vault-file-dir")
                        .description("If set, secrets can be obtained by reading the content of files within the given directory.")
                        .paramLabel("dir")
                        .build(),
                builder()
                        .from("vault-")
                        .to("quarkus.vault.")
                        .description("Maps any vault option to their corresponding properties in quarkus-vault extension.")
                        .hidden(true)
                        .isBuildTimeProperty(true)
                        .build(),
                builder()
                        .from("vault-url")
                        .to("quarkus.vault.url")
                        .description("The vault server url.")
                        .paramLabel("paths")
                        .hidden(true)
                        .isBuildTimeProperty(true)
                        .build(),
                builder()
                        .from("vault-kv-paths")
                        .to("kc.spi-vault-hashicorp-paths")
                        .description("A set of one or more key/value paths that should be used when looking up secrets.")
                        .paramLabel("paths")
                        .hidden(true)
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.VAULT);
    }
}
