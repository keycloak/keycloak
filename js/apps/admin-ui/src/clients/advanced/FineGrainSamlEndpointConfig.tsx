import { ActionGroup, Button } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { ApplicationUrls } from "./ApplicationUrls";

type FineGrainSamlEndpointConfigProps = {
  save: () => void;
  reset: () => void;
};

export const FineGrainSamlEndpointConfig = ({
  save,
  reset,
}: FineGrainSamlEndpointConfigProps) => {
  const { t } = useTranslation();
  return (
    <FormAccess role="manage-realm" isHorizontal>
      <ApplicationUrls />
      <TextControl
        name="attributes.saml_assertion_consumer_url_post"
        label={t("assertionConsumerServicePostBindingURL")}
        labelIcon={t("assertionConsumerServicePostBindingURLHelp")}
        type="url"
      />
      <TextControl
        name="attributes.saml_assertion_consumer_url_redirect"
        label={t("assertionConsumerServiceRedirectBindingURL")}
        labelIcon={t("assertionConsumerServiceRedirectBindingURLHelp")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_post"
        label={t("logoutServicePostBindingURL")}
        labelIcon={t("logoutServicePostBindingURLHelp")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_redirect"
        label={t("logoutServiceRedirectBindingURL")}
        labelIcon={t("logoutServiceRedirectBindingURLHelp")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_soap"
        label={t("logoutServiceSoapBindingUrl")}
        labelIcon={t("logoutServiceSoapBindingUrlHelp")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_artifact"
        label={t("logoutServiceArtifactBindingUrl")}
        labelIcon={t("logoutServiceArtifactBindingUrlHelp")}
        type="url"
      />
      <TextControl
        name="attributes.saml_artifact_binding_url"
        label={t("artifactBindingUrl")}
        labelIcon={t("artifactBindingUrlHelp")}
        type="url"
      />
      <TextControl
        name="attributes.saml_artifact_resolution_service_url"
        label={t("artifactResolutionService")}
        labelIcon={t("artifactResolutionServiceHelp")}
        type="url"
      />

      <ActionGroup>
        <Button variant="tertiary" onClick={save} data-testid="fineGrainSave">
          {t("save")}
        </Button>
        <Button variant="link" onClick={reset} data-testid="fineGrainRevert">
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
