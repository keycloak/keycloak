import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";

import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { KeycloakTextArea } from "../../../components/keycloak-text-area/KeycloakTextArea";

type NameDescriptionProps = {
  prefix: string;
  isDisabled: boolean;
};

export const NameDescription = ({
  prefix,
  isDisabled,
}: NameDescriptionProps) => {
  const { t } = useTranslation();
  const {
    register,
    formState: { errors },
  } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("name")}
        fieldId="kc-name"
        helperTextInvalid={t("required")}
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        isRequired
        labelIcon={
          <HelpItem helpText={t(`${prefix}-nameHelp`)} fieldLabelId="name" />
        }
      >
        <KeycloakTextInput
          isDisabled={isDisabled}
          id="kc-name"
          data-testid="name"
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          {...register("name", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("description")}
        fieldId="kc-description"
        labelIcon={
          <HelpItem
            helpText={t(`${prefix}-descriptionHelp`)}
            fieldLabelId="description"
          />
        }
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("maxLength", { length: 255 })}
      >
        <KeycloakTextArea
          isDisabled={isDisabled}
          id="kc-description"
          data-testid="description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          {...register("description", { maxLength: 255 })}
        />
      </FormGroup>
    </>
  );
};
