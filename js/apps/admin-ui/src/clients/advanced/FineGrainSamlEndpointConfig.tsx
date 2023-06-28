import { ActionGroup, Button } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { TextControl } from "ui-shared";
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
  return (
    <FormAccess role="manage-realm" isHorizontal>
      <ApplicationUrls />
      <TextControl
        name="attributes.saml_assertion_consumer_url_post"
        label={t("assertionConsumerServicePostBindingURL")}
        labelIcon={t("clients-help:assertionConsumerServicePostBindingURL")}
        type="url"
      />
      <TextControl
        name="attributes.saml_assertion_consumer_url_redirect"
        label={t("assertionConsumerServiceRedirectBindingURL")}
        labelIcon={t("clients-help:assertionConsumerServiceRedirectBindingURL")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_post"
        label={t("logoutServicePostBindingURL")}
        labelIcon={t("clients-help:logoutServicePostBindingURL")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_redirect"
        label={t("logoutServiceRedirectBindingURL")}
        labelIcon={t("clients-help:logoutServiceRedirectBindingURL")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_soap"
        label={t("logoutServiceSoapBindingUrl")}
        labelIcon={t("clients-help:logoutServiceSoapBindingUrl")}
        type="url"
      />
      <TextControl
        name="attributes.saml_single_logout_service_url_artifact"
        label={t("logoutServiceArtifactBindingUrl")}
        labelIcon={t("clients-help:logoutServiceArtifactBindingUrl")}
        type="url"
      />
      <TextControl
        name="attributes.saml_artifact_binding_url"
        label={t("artifactBindingUrl")}
        labelIcon={t("clients-help:artifactBindingUrl")}
        type="url"
      />
      <TextControl
        name="attributes.saml_artifact_resolution_service_url"
        label={t("artifactResolutionService")}
        labelIcon={t("clients-help:artifactResolutionService")}
        type="url"
      />

      <ActionGroup>
        <Button variant="tertiary" onClick={save} data-testid="fineGrainSave">
          {t("common:save")}
        </Button>
        <Button variant="link" onClick={reset} data-testid="fineGrainRevert">
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
