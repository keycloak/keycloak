import { useState } from "react";

import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import KeysMetadataRepresentation from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";

import { adminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useFetch } from "../../utils/useFetch";

import { ProviderInstanceSelect } from "../provider-instances/ProviderInstanceSelect";
import { convertToName } from "./DynamicComponents";
import type { ComponentProps } from "./components";

export const KeyComponent = (props: ComponentProps) => {
  const { realm } = useRealm();

  const [keys, setKeys] = useState<KeyMetadataRepresentation[]>([]);

  const isMultiSelect = props.options?.some((option) => option == "multi");

  useFetch(
    async () => adminClient.realms.getKeys({ realm }),
    (values: KeysMetadataRepresentation) => values.keys && setKeys(values.keys),
    [realm],
  );

  const getKeyInfo = (
    option: ComponentRepresentation,
  ): KeyMetadataRepresentation | undefined =>
    keys.findLast((k) => k.providerId == option?.id);

  const filter = (options: ComponentRepresentation[]) =>
    options.filter((option) => !!getKeyInfo(option));

  const getDisplayName = (option: ComponentRepresentation) => {
    const keyInfo = getKeyInfo(option);
    return `${option.name} (${keyInfo?.kid})`;
  };

  return (
    <ProviderInstanceSelect
      key={keys.toString()}
      {...props}
      name={convertToName(props.name!)}
      onChange={(selected: ComponentRepresentation[]): string[] =>
        selected.map((c) => getKeyInfo(c)?.kid!)
      }
      onLoad={(loaded: any[], options) =>
        options.filter((option) => loaded.includes(getKeyInfo(option)?.kid))
      }
      providerType="org.keycloak.keys.KeyProvider"
      multiSelect={isMultiSelect!}
      filterOptions={filter}
      getDisplayName={getDisplayName}
    />
  );
};
