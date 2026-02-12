import {
  PasswordControl,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";

export const SsfReceiverSettings = () => {
  const { t } = useTranslation();

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

      <PasswordControl
        name="config.transmitterToken"
        label={t("ssfTransmitterToken")}
        labelIcon={t("ssfTransmitterTokenHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <SelectControl
        name="config.transmitterTokenType"
        label={t("ssfTransmitterTokenType")}
        labelIcon={t("ssfTransmitterTokenTypeHelp")}
        options={[
          {
            key: "ACCESS_TOKEN",
            value: t("ssfTransmitterTokenType.accessToken"),
          },
        ]}
        controller={{ defaultValue: "ACCESS_TOKEN" }}
      />
    </>
  );
};
