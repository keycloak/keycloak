import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

export const ApplicationUrls = () => {
  const { t } = useTranslation("clients");
  const { register } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("logoUrl")}
        fieldId="logoUrl"
        labelIcon={
          <HelpItem
            helpText="clients-help:logoUrl"
            fieldLabelId="clients:logoUrl"
          />
        }
      >
        <KeycloakTextInput
          id="logoUrl"
          type="url"
          data-testid="logoUrl"
          {...register(
            convertAttributeNameToForm<FormFields>("attributes.logoUri")
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("policyUrl")}
        fieldId="policyUrl"
        labelIcon={
          <HelpItem
            helpText="clients-help:policyUrl"
            fieldLabelId="clients:policyUrl"
          />
        }
      >
        <KeycloakTextInput
          id="policyUrl"
          data-testid="policyUrl"
          type="url"
          {...register(
            convertAttributeNameToForm<FormFields>("attributes.policyUri")
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("termsOfServiceUrl")}
        fieldId="termsOfServiceUrl"
        labelIcon={
          <HelpItem
            helpText="clients-help:termsOfServiceUrl"
            fieldLabelId="clients:termsOfServiceUrl"
          />
        }
      >
        <KeycloakTextInput
          id="termsOfServiceUrl"
          type="url"
          data-testid="termsOfServiceUrl"
          {...register(
            convertAttributeNameToForm<FormFields>("attributes.tosUri")
          )}
        />
      </FormGroup>
    </>
  );
};
