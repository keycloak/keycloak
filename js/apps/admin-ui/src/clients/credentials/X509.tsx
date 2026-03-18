import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

export const X509 = () => {
  const { t } = useTranslation();
  return (
    <>
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.x509.allow.regex.pattern.comparison",
        )}
        label={t("allowRegexComparison")}
        labelIcon={t("allowRegexComparisonHelp")}
        stringify
      />
      <TextControl
        name={convertAttributeNameToForm("attributes.x509.subjectdn")}
        label={t("subject")}
        labelIcon={t("subjectHelp")}
        rules={{
          required: t("required"),
        }}
      />
    </>
  );
};
