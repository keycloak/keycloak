import { FormGroup, Title } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form-v7";

import { useTranslation } from "react-i18next";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { DiscoveryEndpointField } from "../component/DiscoveryEndpointField";
import { DiscoverySettings } from "./DiscoverySettings";

export const OpenIdConnectSettings = () => {
  const { t } = useTranslation("identity-providers");
  const id = "oidc";

  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const {
    setValue,
    setError,
    clearErrors,
    formState: { errors },
  } = useFormContext();

  const setupForm = (result: any) => {
    Object.keys(result).map((k) => setValue(`config.${k}`, result[k]));
  };

  const fileUpload = async (obj?: object) => {
    clearErrors("discoveryError");
    if (!obj) {
      return;
    }

    const formData = new FormData();
    formData.append("providerId", id);
    formData.append("file", new Blob([JSON.stringify(obj)]));

    try {
      const response = await fetch(
        `${addTrailingSlash(
          adminClient.baseUrl
        )}admin/realms/${realm}/identity-provider/import-config`,
        {
          method: "POST",
          body: formData,
          headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
        }
      );
      if (response.ok) {
        const result = await response.json();
        setupForm(result);
      } else {
        setError("discoveryError", {
          type: "manual",
          message: response.statusText,
        });
      }
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
            helperTextInvalid={errors.discoveryError?.message}
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
