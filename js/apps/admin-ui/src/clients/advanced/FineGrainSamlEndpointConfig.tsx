import { ActionGroup, Button, FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ApplicationUrls } from "./ApplicationUrls";

type FineGrainSamlEndpointConfigProps = {
  save: () => void;
  reset: () => void;
};

export const FineGrainSamlEndpointConfig = ({
  save,
  reset,
}: FineGrainSamlEndpointConfigProps) => {
  const { t } = useTranslation("clients");
  const { register } = useFormContext();
  return (
    <FormAccess role="manage-realm" isHorizontal>
      <ApplicationUrls />
      <FormGroup
        label={t("assertionConsumerServicePostBindingURL")}
        fieldId="assertionConsumerServicePostBindingURL"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:assertionConsumerServicePostBindingURL")}
            fieldLabelId="clients:assertionConsumerServicePostBindingURL"
          />
        }
      >
        <KeycloakTextInput
          id="assertionConsumerServicePostBindingURL"
          type="url"
          {...register("attributes.saml_assertion_consumer_url_post")}
        />
      </FormGroup>
      <FormGroup
        label={t("assertionConsumerServiceRedirectBindingURL")}
        fieldId="assertionConsumerServiceRedirectBindingURL"
        labelIcon={
          <HelpItem
            helpText={t(
              "clients-help:assertionConsumerServiceRedirectBindingURL"
            )}
            fieldLabelId="clients:assertionConsumerServiceRedirectBindingURL"
          />
        }
      >
        <KeycloakTextInput
          id="assertionConsumerServiceRedirectBindingURL"
          type="url"
          {...register("attributes.saml_assertion_consumer_url_redirect")}
        />
      </FormGroup>
      <FormGroup
        label={t("logoutServicePostBindingURL")}
        fieldId="logoutServicePostBindingURL"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:logoutServicePostBindingURL")}
            fieldLabelId="clients:logoutServicePostBindingURL"
          />
        }
      >
        <KeycloakTextInput
          id="logoutServicePostBindingURL"
          type="url"
          {...register("attributes.saml_single_logout_service_url_post")}
        />
      </FormGroup>
      <FormGroup
        label={t("logoutServiceRedirectBindingURL")}
        fieldId="logoutServiceRedirectBindingURL"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:logoutServiceRedirectBindingURL")}
            fieldLabelId="clients:logoutServiceRedirectBindingURL"
          />
        }
      >
        <KeycloakTextInput
          id="logoutServiceRedirectBindingURL"
          type="url"
          {...register("attributes.saml_single_logout_service_url_redirect")}
        />
      </FormGroup>
      <FormGroup
        label={t("logoutServiceArtifactBindingUrl")}
        fieldId="logoutServiceArtifactBindingUrl"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:logoutServiceArtifactBindingUrl")}
            fieldLabelId="clients:logoutServiceArtifactBindingUrl"
          />
        }
      >
        <KeycloakTextInput
          id="logoutServiceArtifactBindingUrl"
          type="url"
          {...register("attributes.saml_single_logout_service_url_artifact")}
        />
      </FormGroup>
      <FormGroup
        label={t("artifactBindingUrl")}
        fieldId="artifactBindingUrl"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:artifactBindingUrl")}
            fieldLabelId="clients:artifactBindingUrl"
          />
        }
      >
        <KeycloakTextInput
          id="artifactBindingUrl"
          type="url"
          {...register("attributes.saml_artifact_binding_url")}
        />
      </FormGroup>
      <FormGroup
        label={t("artifactResolutionService")}
        fieldId="artifactResolutionService"
        labelIcon={
          <HelpItem
            helpText={t("clients-help:artifactResolutionService")}
            fieldLabelId="clients:artifactResolutionService"
          />
        }
      >
        <KeycloakTextInput
          id="artifactResolutionService"
          type="url"
          {...register("attributes.saml_artifact_resolution_service_url")}
        />
      </FormGroup>

      <ActionGroup>
        <Button variant="tertiary" onClick={save}>
          {t("common:save")}
        </Button>
        <Button variant="link" onClick={reset}>
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
