package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.VaultOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class VaultPropertyMappers {

    private VaultPropertyMappers() {
    }

    public static PropertyMapper[] getVaultPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(VaultOptions.VAULT)
                        .paramLabel("provider")
                        .build(),
                fromOption(VaultOptions.VAULT_DIR)
                        .to("kc.spi-vault-file-dir")
                        .paramLabel("dir")
                        .build(),
                fromOption(VaultOptions.VAULT_UNMAPPED)
                        .to("quarkus.vault.")
                        .build(),
                fromOption(VaultOptions.VAULT_URL)
                        .to("quarkus.vault.url")
                        .paramLabel("paths")
                        .build(),
                fromOption(VaultOptions.VAULT_KV_PATHS)
                        .to("kc.spi-vault-hashicorp-paths")
                        .paramLabel("paths")
                        .build()
        };
    }

}
