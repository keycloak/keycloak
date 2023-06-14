import { useTranslation } from "react-i18next";
import { TextControl } from "ui-shared";

type ApplicationUrlsProps = {
  isDisabled?: boolean;
};

export const ApplicationUrls = (props: ApplicationUrlsProps) => {
  const { t } = useTranslation("clients");

  return (
    <>
      <TextControl
        name="attributes.logoUri"
        label={t("clients:logoUrl")}
        labelIcon={t("clients-help:logoUrl")}
        type="url"
        {...props}
      />
      <TextControl
        name="attributes.policyUri"
        label={t("clients:policyUrl")}
        labelIcon={t("clients-help:policyUrl")}
        type="url"
        {...props}
      />
      <TextControl
        name="attributes.tosUri"
        label={t("clients:termsOfServiceUrl")}
        labelIcon={t("clients-help:termsOfServiceUrl")}
        type="url"
        {...props}
      />
    </>
  );
};
