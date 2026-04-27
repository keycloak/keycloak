import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
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
];

export const OIDCAuthentication = ({ create = true }: { create?: boolean }) => {
  const providers = useServerInfo().providers!.clientSignature.providers;
  const { t } = useTranslation();

  const { control } = useFormContext();

  const clientAuthMethod = useWatch({
    control: control,
    name: "config.clientAuthMethod",
  });

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
        secretRequired={clientAuthMethod !== "private_key_jwt"}
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
    </>
  );
};
