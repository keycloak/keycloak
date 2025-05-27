import { FormGroup, Title } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormErrorText, HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { DiscoveryEndpointField } from "../component/DiscoveryEndpointField";
import { DiscoverySettings } from "./DiscoverySettings";

type OpenIdConnectSettingsProps = {
  isOIDC: boolean;
};

export const OpenIdConnectSettings = ({
  isOIDC,
}: OpenIdConnectSettingsProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const id = "oidc";

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
      const result =
        await adminClient.identityProviders.importFromUrl(formData);
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
      <Title headingLevel="h2" size="xl" className="kc-form-panel__title">
        {isOIDC ? t("oidcSettings") : t("oAuthSettings")}
      </Title>

      <DiscoveryEndpointField
        id="oidc"
        fileUpload={
          <FormGroup
            label={t("importConfig")}
            fieldId="kc-import-config"
            labelIcon={
              <HelpItem
                helpText={t("importConfigHelp")}
                fieldLabelId="importConfig"
              />
            }
          >
            <JsonFileUpload
              id="kc-import-config"
              helpText={t("identity=providers-help:jsonFileUpload")}
              hideDefaultPreview
              unWrap
              validated={errors.discoveryError ? "error" : "default"}
              onChange={(value) => fileUpload(value)}
            />
            {errors.discoveryError && (
              <FormErrorText
                message={errors.discoveryError.message as string}
              />
            )}
          </FormGroup>
        }
      >
        {(readonly) => (
          <DiscoverySettings readOnly={readonly} isOIDC={isOIDC} />
        )}
      </DiscoveryEndpointField>
    </>
  );
};
