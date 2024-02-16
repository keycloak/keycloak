import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { Checkbox, FormGroup } from "@patternfly/react-core";

import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";

export const Regex = () => {
  const { t } = useTranslation();
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("targetClaim")}
        fieldId="targetClaim"
        helperTextInvalid={t("required")}
        validated={errors.targetClaim ? "error" : "default"}
        isRequired
        labelIcon={
          <HelpItem
            helpText={t("targetClaimHelp")}
            fieldLabelId="targetClaim"
          />
        }
      >
        <KeycloakTextInput
          id="targetClaim"
          data-testid="targetClaim"
          validated={errors.targetClaim ? "error" : "default"}
          {...register("targetClaim", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("regexPattern")}
        fieldId="pattern"
        labelIcon={
          <HelpItem
            helpText={t("regexPatternHelp")}
            fieldLabelId="regexPattern"
          />
        }
        isRequired
        validated={errors.pattern ? "error" : "default"}
        helperTextInvalid={t("required")}
      >
        <KeycloakTextInput
          id="pattern"
          data-testid="regexPattern"
          validated={errors.pattern ? "error" : "default"}
          {...register("pattern", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("targetContextAttributes")}
        fieldId="targetContextAttributes"
        labelIcon={
          <HelpItem
            helpText={t("targetContextAttributesHelp")}
            fieldLabelId="targetContextAttributes"
          />
        }
      >
        <Controller
          name="targetContextAttributes"
          defaultValue={false}
          control={control}
          render={({ field }) => (
            <Checkbox
              id="targetContextAttributes"
              isChecked={field.value}
              onChange={field.onChange}
            />
          )}
        />
      </FormGroup>
    </>
  );
};
