import { useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import { useRealmKeys } from "./useRealmKeys";

type SigningKeySelectProps = {
  name: string;
  protocol: "openid-connect" | "saml";
  label: string;
  labelIcon: string;
  // When several selectors share one parent (the OIDC advanced tab renders four), the
  // parent fetches the realm keys once with useRealmKeys() and passes them in to avoid
  // duplicate requests. Omit for standalone usage (e.g. the single SAML selector), in which
  // case the component fetches them itself. `undefined` means "not provided"; an empty array
  // means "fetched, but the realm has no keys".
  realmKeys?: KeyMetadataRepresentation[];
  canViewRealmKeys?: boolean;
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
  realmKeys: providedRealmKeys,
  canViewRealmKeys: providedCanViewRealmKeys,
}: SigningKeySelectProps) => {
  const { t } = useTranslation();
  const currentValue = useWatch({ name });

  // Fetch internally only when a parent hasn't already provided the keys. When the admin
  // can't list the realm keys, the selector degrades to read-only and preserves the
  // currently configured value.
  const fetched = useRealmKeys(providedRealmKeys === undefined);
  const realmKeys = providedRealmKeys ?? fetched.realmKeys;
  const canViewRealmKeys = providedCanViewRealmKeys ?? fetched.canViewRealmKeys;

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

  // If the configured key is not in the eligible list, it is either deleted or exists but
  // is ineligible for this protocol (wrong type/algorithm). Either way the server will not
  // use it — it falls back to the realm's active key — so render it as "Not found" rather
  // than surfacing its real (misleading) status metadata as if it were usable.
  const notFound = (() => {
    if (!currentValue || filtered.some((o) => o.key === currentValue))
      return [];
    return [toOption(currentValue)];
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
