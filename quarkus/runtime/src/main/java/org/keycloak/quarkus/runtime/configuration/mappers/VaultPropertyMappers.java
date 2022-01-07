package org.keycloak.quarkus.runtime.configuration.mappers;

final class VaultPropertyMappers {

    private VaultPropertyMappers() {
    }

    public static PropertyMapper[] getVaultPropertyMappers() {
        return new PropertyMapper[] {
                builder()
                        .from("vault.file.path")
                        .to("kc.spi.vault.files-plaintext.dir")
                        .description("If set, secrets can be obtained by reading the content of files within the given path.")
                        .paramLabel("dir")
                        .build(),
                builder()
                        .from("vault.hashicorp.")
                        .to("quarkus.vault.")
                        .description("If set, secrets can be obtained from Hashicorp Vault.")
                        .build(),
                builder()
                        .from("vault.hashicorp.paths")
                        .to("kc.spi.vault.hashicorp.paths")
                        .description("A set of one or more paths that should be used when looking up secrets.")
                        .paramLabel("paths")
                        .build()
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.VAULT).isBuildTimeProperty(true);
    }
}
