import {
  PasswordControl,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { useWatch } from "react-hook-form";
import { ClientIdSecret } from "../component/ClientIdSecret";

export const SsfReceiverSettings = () => {
  const { t } = useTranslation();

  const transmitterAuthMethod = useWatch({
    name: "config.transmitterAuthMethod",
    defaultValue: "STATIC_TOKEN",
  });

  return (
    <>
      <TextControl
        name="alias"
        label={t("alias")}
        labelIcon={t("aliasHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl
        name="config.description"
        label={t("description")}
        labelIcon={t("descriptionHelp")}
        rules={{}}
      />

      <TextControl
        name="config.issuer"
        label={t("issuer")}
        labelIcon={t("ssfTransmitterIssuerHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl
        name="config.transmitterMetadataUrl"
        label={t("transmitterMetadataUrl")}
        labelIcon={t("transmitterMetadataUrlHelp")}
        rules={{}}
      />

      <SelectControl
        name="config.transmitterAuthMethod"
        label={t("ssfTransmitterAuthMethod")}
        labelIcon={t("ssfTransmitterAuthMethodHelp")}
        options={[
          {
            key: "STATIC_TOKEN",
            value: t("ssfTransmitterAuthMethod.staticToken"),
          },
          {
            key: "CLIENT_CREDENTIALS",
            value: t("ssfTransmitterAuthMethod.clientCredentials"),
          },
        ]}
        controller={{ defaultValue: "STATIC_TOKEN" }}
      />

      {(!transmitterAuthMethod || transmitterAuthMethod === "STATIC_TOKEN") && (
        <PasswordControl
          name="config.transmitterToken"
          label={t("ssfTransmitterToken")}
          labelIcon={t("ssfTransmitterTokenHelp")}
          rules={{
            required: t("required"),
          }}
        />
      )}

      {transmitterAuthMethod === "CLIENT_CREDENTIALS" && (
        <>
          <TextControl
            name="config.tokenUrl"
            label={t("tokenUrl")}
            labelIcon={t("ssfTokenUrlHelp")}
            rules={{
              required: t("required"),
            }}
          />

          <SelectControl
            name="config.clientAuthMethod"
            label={t("clientAuthentication")}
            options={[
              {
                key: "client_secret_post",
                value: t("clientAuthentications.client_secret_post"),
              },
              {
                key: "client_secret_basic",
                value: t("clientAuthentications.client_secret_basic"),
              },
            ]}
            controller={{ defaultValue: "client_secret_post" }}
          />

          <ClientIdSecret />

          <TextControl
            name="config.scope"
            label={t("ssfScope")}
            labelIcon={t("ssfScopeHelp")}
            rules={{}}
          />
        </>
      )}
    </>
  );
};
