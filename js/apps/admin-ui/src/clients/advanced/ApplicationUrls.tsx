import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

type ApplicationUrlsProps = {
  isDisabled?: boolean;
};

export const ApplicationUrls = (props: ApplicationUrlsProps) => {
  const { t } = useTranslation("clients");
  const { register } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("logoUrl")}
        fieldId="logoUrl"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:logoUrl")}
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
          {...props}
        />
      </FormGroup>
      <FormGroup
        label={t("policyUrl")}
        fieldId="policyUrl"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:policyUrl")}
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
          {...props}
        />
      </FormGroup>
      <FormGroup
        label={t("termsOfServiceUrl")}
        fieldId="termsOfServiceUrl"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:termsOfServiceUrl")}
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
          {...props}
        />
      </FormGroup>
    </>
  );
};
