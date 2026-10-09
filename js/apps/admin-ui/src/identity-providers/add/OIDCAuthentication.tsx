import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import { SelectControl, useFetch } from "@keycloak/keycloak-ui-shared";
import { useMemo, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { sortProviders } from "../../util";
import { ClientIdSecret } from "../component/ClientIdSecret";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

const clientAuthentications = [
  "client_secret_post",
  "client_secret_basic",
  "client_secret_basic_unencoded",
  "client_secret_jwt",
  "private_key_jwt",
  "tls_client_auth",
];

export const OIDCAuthentication = ({ create = true }: { create?: boolean }) => {
  const providers = useServerInfo().providers!.clientSignature.providers;
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const { control } = useFormContext();

  const clientAuthMethod = useWatch({
    control: control,
    name: "config.clientAuthMethod",
  });

  const [realmKeys, setRealmKeys] = useState<KeyMetadataRepresentation[]>([]);

  // The realm keys are only needed to populate the certificate selector for tls_client_auth.
  // Only fetch them when that method is selected: the /keys endpoint requires view-realm, while
  // managing identity providers only requires manage-identity-providers. Fetching unconditionally
  // would make every OIDC IdP form fail for least-privileged IdP administrators. Any error (e.g. a
  // 403 for admins without view-realm) is swallowed to an empty list; the render then falls back to
  // a free-text provider-ID field so the form stays usable instead of exposing an empty dropdown.
  useFetch(
    async () => {
      if (clientAuthMethod !== "tls_client_auth") {
        return [];
      }
      try {
        const keysMetadata = await adminClient.realms.getKeys({ realm });
        return keysMetadata.keys ?? [];
      } catch (error) {
        console.warn(
          "Could not load realm keys for tls_client_auth certificate selection.",
          error,
        );
        return [];
      }
    },
    setRealmKeys,
    [clientAuthMethod],
  );

  const certKeyProviderOptions = useMemo(() => {
    const seen = new Set<string>();
    const options: { key: string; value: string }[] = [];
    for (const k of realmKeys) {
      // Only ACTIVE/PASSIVE keys are usable: IdpClientCertificateResolver rejects DISABLED keys
      // (KeyStatus.isEnabled), so offering them here would let the UI select a value that is
      // guaranteed to fail on save.
      const usable = k.status === "ACTIVE" || k.status === "PASSIVE";
      if (usable && k.certificate && k.providerId && !seen.has(k.providerId)) {
        seen.add(k.providerId);
        const label = `${k.providerId} (${k.type ?? k.algorithm ?? ""}${k.kid ? ", " + k.kid : ""})`;
        options.push({ key: k.providerId, value: label });
      }
    }
    return options;
  }, [realmKeys]);

  return (
    <>
      <SelectControl
        name="config.clientAuthMethod"
        label={t("clientAuthentication")}
        labelIcon={t("clientAuthenticationHelp")}
        options={clientAuthentications.map((auth) => ({
          key: auth,
          value: t(`clientAuthentications.${auth}`),
        }))}
        controller={{
          defaultValue: clientAuthentications[0],
        }}
      />
      <ClientIdSecret
        secretRequired={
          clientAuthMethod !== "private_key_jwt" &&
          clientAuthMethod !== "tls_client_auth"
        }
        create={create}
      />
      <SelectControl
        name="config.clientAssertionSigningAlg"
        label={t("clientAssertionSigningAlg")}
        labelIcon={t("clientAssertionSigningAlgHelp")}
        options={[
          { key: "", value: t("algorithmNotSpecified") },
          ...sortProviders(providers).map((p) => ({ key: p, value: p })),
        ]}
        controller={{
          defaultValue: "",
        }}
      />
      {(clientAuthMethod === "private_key_jwt" ||
        clientAuthMethod === "client_secret_jwt") && (
        <TextField
          field="config.clientAssertionAudience"
          label="clientAssertionAudience"
        />
      )}
      {clientAuthMethod === "private_key_jwt" && (
        <SwitchField
          field="config.jwtX509HeadersEnabled"
          label="jwtX509HeadersEnabled"
        />
      )}
      {clientAuthMethod === "tls_client_auth" &&
        (certKeyProviderOptions.length > 0 ? (
          <SelectControl
            name="config.clientCertKeyProviderId"
            label={t("clientCertKeyProviderId")}
            labelIcon={t("clientCertKeyProviderIdHelp")}
            options={certKeyProviderOptions}
            controller={{ defaultValue: "" }}
          />
        ) : (
          // When no realm keys with a certificate are available for selection (for example an
          // administrator with manage-identity-providers but without view-realm gets a 403 from the
          // /keys endpoint, or the realm simply has no certificate-bearing key yet), fall back to a
          // free-text field so the provider ID can still be entered. The backend requires a non-empty
          // value, so an empty, unusable dropdown would otherwise block configuration entirely.
          <TextField
            field="config.clientCertKeyProviderId"
            label="clientCertKeyProviderId"
          />
        ))}
    </>
  );
};
