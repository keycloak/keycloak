import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";

type ApplicationUrlsProps = {
  isDisabled?: boolean;
};

export const ApplicationUrls = (props: ApplicationUrlsProps) => {
  const { t } = useTranslation();

  return (
    <>
      <TextControl
        name="attributes.logoUri"
        label={t("logoUrl")}
        labelIcon={t("logoUrlHelp")}
        type="url"
        {...props}
      />
      <TextControl
        name="attributes.policyUri"
        label={t("policyUrl")}
        labelIcon={t("policyUrlHelp")}
        type="url"
        {...props}
      />
      <TextControl
        name="attributes.tosUri"
        label={t("termsOfServiceUrl")}
        labelIcon={t("termsOfServiceUrlHelp")}
        type="url"
        {...props}
      />
    </>
  );
};
