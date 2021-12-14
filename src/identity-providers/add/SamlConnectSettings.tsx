import React from "react";
import { useFormContext } from "react-hook-form";
import { FormGroup, TextInput, Title } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../context/auth/AdminClient";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";

import { FileUploadForm } from "../../components/json-file-upload/FileUploadForm";
import { useRealm } from "../../context/realm-context/RealmContext";
import { DescriptorSettings } from "./DescriptorSettings";
import { getBaseUrl } from "../../util";
import { DiscoveryEndpointField } from "../component/DiscoveryEndpointField";

export const SamlConnectSettings = () => {
  const { t } = useTranslation("identity-providers");
  const id = "saml";

  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { setValue, register, errors, setError } = useFormContext();

  const setupForm = (result: IdentityProviderRepresentation) => {
    Object.entries(result).map(([key, value]) =>
      setValue(`config.${key}`, value)
    );
  };

  const fileUpload = async (xml: string) => {
    const formData = new FormData();
    formData.append("providerId", id);
    formData.append("file", new Blob([xml]));

    try {
      const response = await fetch(
        `${getBaseUrl(
          adminClient
        )}admin/realms/${realm}/identity-provider/import-config`,
        {
          method: "POST",
          body: formData,
          headers: {
            Authorization: `bearer ${await adminClient.getAccessToken()}`,
          },
        }
      );
      const result = await response.json();
      setupForm(result);
    } catch (error) {
      setError("discoveryError", {
        type: "manual",
        message: (error as Error).message,
      });
    }
  };

  return (
    <>
      <Title headingLevel="h4" size="xl" className="kc-form-panel__title">
        {t("samlSettings")}
      </Title>

      <FormGroup
        label={t("serviceProviderEntityId")}
        fieldId="kc-service-provider-entity-id"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:serviceProviderEntityId"
            fieldLabelId="identity-providers:serviceProviderEntityId"
          />
        }
      >
        <TextInput
          type="text"
          name="config.entityId"
          data-testid="serviceProviderEntityId"
          id="kc-service-provider-entity-id"
          ref={register()}
        />
      </FormGroup>

      <DiscoveryEndpointField
        id="saml"
        fileUpload={
          <FormGroup
            label={t("importConfig")}
            fieldId="kc-import-config"
            labelIcon={
              <HelpItem
                helpText="identity-providers-help:importConfig"
                fieldLabelId="identity-providers:importConfig"
              />
            }
            validated={errors.discoveryError ? "error" : "default"}
            helperTextInvalid={errors.discoveryError}
          >
            <FileUploadForm
              id="kc-import-config"
              extension=".xml"
              hideDefaultPreview
              unWrap
              validated={errors.discoveryError ? "error" : "default"}
              onChange={(value) => fileUpload(value)}
            />
          </FormGroup>
        }
      >
        {(readonly) => <DescriptorSettings readOnly={readonly} />}
      </DiscoveryEndpointField>
    </>
  );
};
