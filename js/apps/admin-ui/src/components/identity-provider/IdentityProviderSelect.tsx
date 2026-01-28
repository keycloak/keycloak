import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { IdentityProviderType } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import type { IdentityProvidersQuery } from "@keycloak/keycloak-admin-client/lib/resources/identityProviders";
import { SelectVariant, useFetch } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "../dynamic/components";
import { MultiValuedListComponent } from "../dynamic/MultivaluedListComponent";

type IdentityProviderSelectProps = ComponentProps & {
  variant?: `${SelectVariant}`;
  identityProviderType?: IdentityProviderType;
  realmOnly?: boolean;
};

export const IdentityProviderSelect = ({
  identityProviderType = IdentityProviderType.ANY,
  realmOnly = false,
  ...props
}: IdentityProviderSelectProps) => {
  const { adminClient } = useAdminClient();

  const [identityProviders, setIdentityProviders] = useState<
    IdentityProviderRepresentation[]
  >([]);
  const [search, setSearch] = useState("");

  useFetch(
    () => {
      const params: IdentityProvidersQuery = {
        max: 20,
        type: identityProviderType,
        realmOnly: realmOnly,
      };
      if (search) {
        params.search = search;
      }
      return adminClient.identityProviders.find(params);
    },
    (identityProviders) => setIdentityProviders(identityProviders),
    [search],
  );

  return (
    <MultiValuedListComponent
      {...props}
      onSearch={setSearch}
      options={identityProviders.map(({ alias }) => alias!)}
    />
  );
};
