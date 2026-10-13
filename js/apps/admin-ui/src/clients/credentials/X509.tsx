import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

export const X509 = () => {
  const { t } = useTranslation();
  const { watch } = useFormContext<FormFields>();
  const regexEnabled = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.x509.allow.regex.pattern.comparison",
    ),
    "false",
  );
  return (
    <>
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.x509.allow.regex.pattern.comparison",
        )}
        label={t("allowRegexComparison")}
        labelIcon={
          <HelpItem
            helpText={t("allowRegexComparisonHelp")}
            fieldLabelId="allowRegexComparison"
            isRecommendation={regexEnabled === "true"}
          />
        }
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
      <TextControl
        name={convertAttributeNameToForm("attributes.x509.casubjectdn")}
        label={t("casubject")}
        labelIcon={t("casubjectHelp")}
        rules={{
          required: t("required"),
        }}
      />
    </>
  );
};
