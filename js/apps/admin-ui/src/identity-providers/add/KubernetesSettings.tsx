import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";

export const KubernetesSettings = () => {
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
        name="config.jwksUrl"
        labelIcon={t("kubernetesJWKSURLHelp")}
        label={t("kubernetesJWKSURL")}
      />
    </>
  );
};
