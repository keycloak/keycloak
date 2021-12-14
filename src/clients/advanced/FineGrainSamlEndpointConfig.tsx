import React from "react";
import { useTranslation } from "react-i18next";
import type { Control } from "react-hook-form";
import {
  ActionGroup,
  Button,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";

type FineGrainSamlEndpointConfigProps = {
  control: Control<Record<string, any>>;
  save: () => void;
  reset: () => void;
};

export const FineGrainSamlEndpointConfig = ({
  control: { register },
  save,
  reset,
}: FineGrainSamlEndpointConfigProps) => {
  const { t } = useTranslation("clients");
  return (
    <FormAccess role="manage-realm" isHorizontal>
      <FormGroup
        label={t("assertionConsumerServicePostBindingURL")}
        fieldId="assertionConsumerServicePostBindingURL"
        labelIcon={
          <HelpItem
            helpText="clients-help:assertionConsumerServicePostBindingURL"
            fieldLabelId="clients:assertionConsumerServicePostBindingURL"
          />
        }
      >
        <TextInput
          ref={register()}
          type="text"
          id="assertionConsumerServicePostBindingURL"
          name="attributes.saml_assertion_consumer_url_post"
        />
      </FormGroup>
      <FormGroup
        label={t("assertionConsumerServiceRedirectBindingURL")}
        fieldId="assertionConsumerServiceRedirectBindingURL"
        labelIcon={
          <HelpItem
            helpText="clients-help:assertionConsumerServiceRedirectBindingURL"
            fieldLabelId="clients:assertionConsumerServiceRedirectBindingURL"
          />
        }
      >
        <TextInput
          ref={register()}
          type="text"
          id="assertionConsumerServiceRedirectBindingURL"
          name="attributes.saml_assertion_consumer_url_redirect"
        />
      </FormGroup>
      <FormGroup
        label={t("logoutServicePostBindingURL")}
        fieldId="logoutServicePostBindingURL"
        labelIcon={
          <HelpItem
            helpText="clients-help:logoutServicePostBindingURL"
            fieldLabelId="clients:logoutServicePostBindingURL"
          />
        }
      >
        <TextInput
          ref={register()}
          type="text"
          id="logoutServicePostBindingURL"
          name="attributes.saml_single_logout_service_url_post"
        />
      </FormGroup>
      <FormGroup
        label={t("logoutServiceRedirectBindingURL")}
        fieldId="logoutServiceRedirectBindingURL"
        labelIcon={
          <HelpItem
            helpText="clients-help:logoutServiceRedirectBindingURL"
            fieldLabelId="clients:logoutServiceRedirectBindingURL"
          />
        }
      >
        <TextInput
          ref={register()}
          type="text"
          id="logoutServiceRedirectBindingURL"
          name="attributes.saml_single_logout_service_url_redirect"
        />
      </FormGroup>
      <FormGroup
        label={t("logoutServiceArtifactBindingUrl")}
        fieldId="logoutServiceArtifactBindingUrl"
        labelIcon={
          <HelpItem
            helpText="clients-help:logoutServiceArtifactBindingUrl"
            fieldLabelId="clients:logoutServiceArtifactBindingUrl"
          />
        }
      >
        <TextInput
          ref={register()}
          type="text"
          id="logoutServiceArtifactBindingUrl"
          name="attributes.saml_single_logout_service_url_redirect"
        />
      </FormGroup>
      <FormGroup
        label={t("artifactBindingUrl")}
        fieldId="artifactBindingUrl"
        labelIcon={
          <HelpItem
            helpText="clients-help:artifactBindingUrl"
            fieldLabelId="clients:artifactBindingUrl"
          />
        }
      >
        <TextInput
          ref={register()}
          type="text"
          id="artifactBindingUrl"
          name="attributes.saml_artifact_binding_url"
        />
      </FormGroup>
      <FormGroup
        label={t("artifactResolutionService")}
        fieldId="artifactResolutionService"
        labelIcon={
          <HelpItem
            helpText="clients-help:artifactResolutionService"
            fieldLabelId="clients:artifactResolutionService"
          />
        }
      >
        <TextInput
          ref={register()}
          type="text"
          id="artifactResolutionService"
          name="attributes.saml_artifact_resolution_service_url"
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
