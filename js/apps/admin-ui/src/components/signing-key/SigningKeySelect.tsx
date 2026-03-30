import { useState } from "react";
import { useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SelectControl, useFetch } from "@keycloak/keycloak-ui-shared";
import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";

type SigningKeySelectProps = {
  name: string;
  protocol: "openid-connect" | "saml";
  label: string;
  labelIcon: string;
};

function formatKeyOption(
  kid: string,
  algorithm: string | undefined,
  status: string | undefined,
  t: (key: string) => string,
): string {
  const alg = algorithm ?? "unknown";
  switch (status) {
    case "ACTIVE":
      return `${alg} - ${kid}`;
    case "PASSIVE":
      return `${alg} (${t("signingKeyPassive")}) - ${kid}`;
    case "DISABLED":
      return `${alg} (${t("signingKeyDisabled")}) - ${kid}`;
    default:
      return `${t("signingKeyNotFound")} - ${kid}`;
  }
}

export const SigningKeySelect = ({
  name,
  protocol,
  label,
  labelIcon,
}: SigningKeySelectProps) => {
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const [realmKeys, setRealmKeys] = useState<KeyMetadataRepresentation[]>([]);
  const currentValue = useWatch({ name });

  useFetch(
    async () => {
      const keysMetadata = await adminClient.realms.getKeys({ realm });
      return keysMetadata.keys || [];
    },
    setRealmKeys,
    [],
  );

  const toOption = (kid: string, alg?: string, status?: string) => ({
    key: kid,
    value: formatKeyOption(kid, alg, status, t),
  });

  const isSigningKey = (k: KeyMetadataRepresentation) =>
    k.kid &&
    k.algorithm &&
    k.use === "SIG" &&
    ["ACTIVE", "PASSIVE", "DISABLED"].includes(k.status!);

  // SAML: only RS256 keys are supported (server-side key lookup uses Algorithm.RS256)
  // OIDC: exclude HMAC (OCT) keys as they are derived from client secrets, not realm keys
  const isKeyTypeAllowed = (k: KeyMetadataRepresentation) =>
    protocol === "saml" ? k.algorithm === "RS256" : k.type !== "OCT";

  const filtered = realmKeys
    .filter((k) => isSigningKey(k) && isKeyTypeAllowed(k))
    .map((k) => toOption(k.kid!, k.algorithm, k.status));

  // Show "Not found" entry if the currently selected key has been deleted from the realm
  const notFound = (() => {
    if (!currentValue || filtered.some((o) => o.key === currentValue))
      return [];
    const k = realmKeys.find((k) => k.kid === currentValue);
    return [toOption(currentValue, k?.algorithm, k?.status)];
  })();

  const keyOptions = [
    { key: "", value: t("signingKeyUseRealmActive") },
    ...filtered,
    ...notFound,
  ];

  return (
    <SelectControl
      name={name}
      label={label}
      labelIcon={labelIcon}
      controller={{ defaultValue: "" }}
      options={keyOptions}
    />
  );
};
