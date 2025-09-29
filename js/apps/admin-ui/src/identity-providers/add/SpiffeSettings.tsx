import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";

export const SpiffeSettings = () => {
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
        name="config.issuer"
        label={t("spiffeTrustDomain")}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl
        name="config.bundleEndpoint"
        label={t("spiffeBundleEndpoint")}
        rules={{
          required: t("required"),
        }}
      />
    </>
  );
};
