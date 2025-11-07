import { TextControl } from "@keycloak/keycloak-ui-shared";
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
        rules={{
          required: t("required"),
        }}
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
        name="config.transmitterAccessToken"
        label={t("ssfTransmitterAccessToken")}
        labelIcon={t("ssfTransmitterAccessTokenHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl
        name="config.streamId"
        label={t("ssfStreamId")}
        labelIcon={t("ssfStreamIdHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl
        name="config.pushAuthorizationHeader"
        label={t("ssfPushAuthorizationHeader")}
        labelIcon={t("ssfPushAuthorizationHeaderHelp")}
      />
    </>
  );
};
