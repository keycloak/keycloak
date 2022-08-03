import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";

import { HelpItem } from "../../../components/help-enabler/HelpItem";
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
            helpText={`clients-help:${prefix}-name`}
            fieldLabelId="name"
          />
        }
      >
        <KeycloakTextInput
          type="text"
          id="kc-name"
          name="name"
          data-testid="name"
          ref={register({ required: true })}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
        />
      </FormGroup>
      <FormGroup
        label={t("common:description")}
        fieldId="kc-description"
        labelIcon={
          <HelpItem
            helpText={`clients-help:${prefix}-description`}
            fieldLabelId="description"
          />
        }
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={errors.description?.message}
      >
        <KeycloakTextArea
          ref={register({
            maxLength: {
              value: 255,
              message: t("common:maxLength", { length: 255 }),
            },
          })}
          type="text"
          id="kc-description"
          name="description"
          data-testid="description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        />
      </FormGroup>
    </>
  );
};
