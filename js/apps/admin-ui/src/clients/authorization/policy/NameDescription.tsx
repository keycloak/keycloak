import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";

import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { KeycloakTextArea } from "../../../components/keycloak-text-area/KeycloakTextArea";

type NameDescriptionProps = {
  prefix: string;
};

export const NameDescription = ({ prefix }: NameDescriptionProps) => {
  const { t } = useTranslation("clients");
  const {
    register,
    formState: { errors },
  } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("common:name")}
        fieldId="kc-name"
        helperTextInvalid={t("common:required")}
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        isRequired
        labelIcon={
          <HelpItem
            helpText={t(`clients-help:${prefix}-name`)}
            fieldLabelId="name"
          />
        }
      >
        <KeycloakTextInput
          id="kc-name"
          data-testid="name"
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          {...register("name", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("common:description")}
        fieldId="kc-description"
        labelIcon={
          <HelpItem
            helpText={t(`clients-help:${prefix}-description`)}
            fieldLabelId="description"
          />
        }
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:maxLength", { length: 255 })}
      >
        <KeycloakTextArea
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
