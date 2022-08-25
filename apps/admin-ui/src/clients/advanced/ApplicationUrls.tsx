import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertAttributeNameToForm } from "../../util";

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
          type="text"
          id="logoUrl"
          name={convertAttributeNameToForm("attributes.logoUri")}
          data-testid="logoUrl"
          ref={register}
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
          type="text"
          id="policyUrl"
          name={convertAttributeNameToForm("attributes.policyUri")}
          data-testid="policyUrl"
          ref={register}
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
          type="text"
          id="termsOfServiceUrl"
          name={convertAttributeNameToForm("attributes.tosUri")}
          data-testid="termsOfServiceUrl"
          ref={register}
        />
      </FormGroup>
    </>
  );
};
