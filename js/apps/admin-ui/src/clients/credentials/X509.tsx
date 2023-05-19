import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, Switch, ValidatedOptions } from "@patternfly/react-core";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { beerify, convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

export const X509 = () => {
  const { t } = useTranslation("clients");
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<FormFields>();
  return (
    <>
      <FormGroup
        label={t("allowRegexComparison")}
        labelIcon={
          <HelpItem
            helpText={t("clients-help:allowRegexComparison")}
            fieldLabelId="clients:allowRegexComparison"
          />
        }
        fieldId="allowRegexComparison"
        hasNoPaddingTop
      >
        <Controller
          name={convertAttributeNameToForm<FormFields>(
            "attributes.x509.allow.regex.pattern.comparison"
          )}
          defaultValue="false"
          control={control}
          render={({ field }) => (
            <Switch
              id="allowRegexComparison"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
              aria-label={t("allowRegexComparison")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("subject")}
        fieldId="kc-subject"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:subject")}
            fieldLabelId="clients:subject"
          />
        }
        helperTextInvalid={t("common:required")}
        validated={
          errors.attributes?.[beerify("x509.subjectdn")]
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        isRequired
      >
        <KeycloakTextInput
          type="text"
          id="kc-subject"
          validated={
            errors.attributes?.[beerify("x509.subjectdn")]
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          {...register(
            convertAttributeNameToForm("attributes.x509.subjectdn"),
            { required: true }
          )}
        />
      </FormGroup>
    </>
  );
};
