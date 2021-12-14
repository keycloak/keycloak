import React from "react";
import { useFormContext } from "react-hook-form";
import { FormGroup, Title } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../context/auth/AdminClient";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useRealm } from "../../context/realm-context/RealmContext";
import { DiscoverySettings } from "./DiscoverySettings";
import { getBaseUrl } from "../../util";
import { DiscoveryEndpointField } from "../component/DiscoveryEndpointField";

export const OpenIdConnectSettings = () => {
  const { t } = useTranslation("identity-providers");
  const id = "oidc";

  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { setValue, errors, setError } = useFormContext();

  const setupForm = (result: any) => {
    Object.keys(result).map((k) => setValue(`config.${k}`, result[k]));
  };

  const fileUpload = async (obj?: object) => {
    if (obj) {
      const formData = new FormData();
      formData.append("providerId", id);
      formData.append("file", new Blob([JSON.stringify(obj)]));

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
    }
  };

  return (
    <>
      <Title headingLevel="h4" size="xl" className="kc-form-panel__title">
        {t("oidcSettings")}
      </Title>

      <DiscoveryEndpointField
        id="oidc"
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
            <JsonFileUpload
              id="kc-import-config"
              helpText="identity=providers-help:jsonFileUpload"
              hideDefaultPreview
              unWrap
              validated={errors.discoveryError ? "error" : "default"}
              onChange={(value) => fileUpload(value)}
            />
          </FormGroup>
        }
      >
        {(readonly) => <DiscoverySettings readOnly={readonly} />}
      </DiscoveryEndpointField>
    </>
  );
};
