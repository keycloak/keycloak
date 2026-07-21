import { useState } from "react";
import { useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SelectControl, useFetch } from "@keycloak/keycloak-ui-shared";
import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import { useAdminClient } from "../../admin-client";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";

type SigningKeySelectProps = {
  name: string;
  protocol: "openid-connect" | "saml";
  label: string;
  labelIcon: string;
};

// The kid is a long opaque string; a short prefix is appended to the label so the
// selected option (the collapsed toggle only renders the label, not the description)
// still shows which key is chosen. The full kid is repeated as the option description.
const KID_LABEL_LENGTH = 12;
const truncateKid = (kid: string) =>
  kid.length > KID_LABEL_LENGTH ? `${kid.slice(0, KID_LABEL_LENGTH)}…` : kid;

// Order within a protocol's key list: group by algorithm, then active keys first
// (highest-priority active is the realm's default for that algorithm), then by
// priority descending so the effective signing key surfaces at the top of its group.
const STATUS_ORDER: Record<string, number> = {
  ACTIVE: 0,
  PASSIVE: 1,
  DISABLED: 2,
};

function formatKeyOption(
  kid: string,
  algorithm: string | undefined,
  status: string | undefined,
  priority: number | undefined,
  t: (key: string) => string,
): string {
  const alg = algorithm ?? "unknown";
  const shortKid = truncateKid(kid);
  const prio = `${t("signingKeyPriority")} ${priority ?? 0}`;
  switch (status) {
    case "ACTIVE":
      return `${alg} (${t("signingKeyActive")}, ${prio}) - ${shortKid}`;
    case "PASSIVE":
      return `${alg} (${t("signingKeyPassive")}, ${prio}) - ${shortKid}`;
    case "DISABLED":
      return `${alg} (${t("signingKeyDisabled")}, ${prio}) - ${shortKid}`;
    default:
      return `${t("signingKeyNotFound")} - ${shortKid}`;
  }
}

export const SigningKeySelect = ({
  name,
  protocol,
  label,
  labelIcon,
}: SigningKeySelectProps) => {
  const { adminClient } = useAdminClient();
  const { hasAccess } = useAccess();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const [realmKeys, setRealmKeys] = useState<KeyMetadataRepresentation[]>([]);
  const currentValue = useWatch({ name });

  // Listing realm keys requires realm-level access (the keys endpoint is guarded by
  // view-realm/manage-realm). A delegated client admin (manage-clients only) can open
  // this form but cannot enumerate realm keys, so the selector is shown read-only with
  // the currently configured key preserved.
  const canViewRealmKeys = hasAccess("view-realm") || hasAccess("manage-realm");

  useFetch(
    async () => {
      if (!canViewRealmKeys) {
        return [];
      }
      const keysMetadata = await adminClient.realms.getKeys({ realm });
      return keysMetadata.keys || [];
    },
    setRealmKeys,
    [canViewRealmKeys, realm],
  );

  const toOption = (
    kid: string,
    alg?: string,
    status?: string,
    priority?: number,
  ) => ({
    key: kid,
    value: formatKeyOption(kid, alg, status, priority, t),
    // Full kid shown as the option description (second line); the label carries a short prefix.
    description: kid,
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
    .sort((a, b) => {
      const byAlgorithm = (a.algorithm ?? "").localeCompare(b.algorithm ?? "");
      if (byAlgorithm !== 0) return byAlgorithm;
      const byStatus =
        (STATUS_ORDER[a.status!] ?? 99) - (STATUS_ORDER[b.status!] ?? 99);
      if (byStatus !== 0) return byStatus;
      return (b.providerPriority ?? 0) - (a.providerPriority ?? 0);
    })
    .map((k) => toOption(k.kid!, k.algorithm, k.status, k.providerPriority));

  // Show "Not found" entry if the currently selected key has been deleted from the realm
  const notFound = (() => {
    if (!currentValue || filtered.some((o) => o.key === currentValue))
      return [];
    const k = realmKeys.find((k) => k.kid === currentValue);
    return [
      toOption(currentValue, k?.algorithm, k?.status, k?.providerPriority),
    ];
  })();

  // Without realm access the key list can't be fetched, so keep only the realm-active
  // option plus the currently configured key (shown as its raw kid) to preserve the value.
  const keyOptions = canViewRealmKeys
    ? [
        { key: "", value: t("signingKeyUseRealmActive") },
        ...filtered,
        ...notFound,
      ]
    : [
        { key: "", value: t("signingKeyUseRealmActive") },
        ...(currentValue ? [{ key: currentValue, value: currentValue }] : []),
      ];

  return (
    <SelectControl
      name={name}
      label={label}
      labelIcon={labelIcon}
      controller={{ defaultValue: "" }}
      options={keyOptions}
      isDisabled={!canViewRealmKeys}
    />
  );
};
