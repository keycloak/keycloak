import AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export const NameDescription = () => {
  const { t } = useTranslation("authentication");
  const {
    register,
    formState: { errors },
  } = useFormContext<AuthenticationFlowRepresentation>();

  return (
    <>
      <FormGroup
        label={t("common:name")}
        fieldId="kc-name"
        helperTextInvalid={t("common:required")}
        validated={
          errors.alias ? ValidatedOptions.error : ValidatedOptions.default
        }
        labelIcon={
          <HelpItem helpText="authentication-help:name" fieldLabelId="name" />
        }
        isRequired
      >
        <KeycloakTextInput
          id="kc-name"
          data-testid="name"
          validated={
            errors.alias ? ValidatedOptions.error : ValidatedOptions.default
          }
          {...register("alias", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("common:description")}
        fieldId="kc-description"
        labelIcon={
          <HelpItem
            helpText="authentication-help:description"
            fieldLabelId="description"
          />
        }
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={errors.description?.message}
      >
        <KeycloakTextArea
          id="kc-description"
          data-testid="description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          {...register("description", {
            maxLength: {
              value: 255,
              message: t("common:maxLength", { length: 255 }),
            },
          })}
        />
      </FormGroup>
    </>
  );
};
