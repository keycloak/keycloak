import { FormGroup, Switch } from "@patternfly/react-core";
import { ReactNode, useEffect, useState } from "react";
import { useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useAdminClient } from "../../context/auth/AdminClient";
import environment from "../../environment";

type DiscoveryEndpointFieldProps = {
  id: string;
  fileUpload: ReactNode;
  children: (readOnly: boolean) => ReactNode;
};

export const DiscoveryEndpointField = ({
  id,
  fileUpload,
  children,
}: DiscoveryEndpointFieldProps) => {
  const { t } = useTranslation("identity-providers");

  const { adminClient } = useAdminClient();

  const {
    setValue,
    register,
    setError,
    watch,
    clearErrors,
    formState: { errors },
  } = useFormContext();
  const discoveryUrl = watch("discoveryEndpoint");

  const [discovery, setDiscovery] = useState(true);
  const [discovering, setDiscovering] = useState(false);
  const [discoveryResult, setDiscoveryResult] =
    useState<Record<string, string>>();

  const setupForm = (result: Record<string, string>) => {
    Object.keys(result).map((k) => setValue(`config.${k}`, result[k]));
  };

  useEffect(() => {
    if (!discoveryUrl) {
      setDiscovering(false);
      return;
    }

    (async () => {
      clearErrors("discoveryError");
      try {
        const result = await adminClient.identityProviders.importFromUrl({
          providerId: id,
          fromUrl: discoveryUrl,
        });
        setupForm(result);
        setDiscoveryResult(result);
      } catch (error) {
        setError("discoveryError", {
          type: "manual",
          message: (error as Error).message,
        });
      }

      setDiscovering(false);
    })();
  }, [discovering]);

  return (
    <>
      <FormGroup
        label={t(
          id === "oidc" ? "useDiscoveryEndpoint" : "useEntityDescriptor"
        )}
        fieldId="kc-discovery-endpoint"
        labelIcon={
          <HelpItem
            helpText={`identity-providers-help:${
              id === "oidc" ? "useDiscoveryEndpoint" : "useEntityDescriptor"
            }`}
            fieldLabelId="identity-providers:discoveryEndpoint"
          />
        }
      >
        <Switch
          id="kc-discovery-endpoint-switch"
          label={t("common:on")}
          labelOff={t("common:off")}
          isChecked={discovery}
          onChange={(checked) => {
            clearErrors("discoveryError");
            setDiscovery(checked);
          }}
          aria-label={t(
            id === "oidc" ? "useDiscoveryEndpoint" : "useEntityDescriptor"
          )}
        />
      </FormGroup>
      {discovery && (
        <FormGroup
          label={t(
            id === "oidc" ? "discoveryEndpoint" : "samlEntityDescriptor"
          )}
          fieldId="kc-discovery-endpoint"
          labelIcon={
            <HelpItem
              helpText={`identity-providers-help:${
                id === "oidc" ? "discoveryEndpoint" : "samlEntityDescriptor"
              }`}
              fieldLabelId="identity-providers:discoveryEndpoint"
            />
          }
          validated={
            errors.discoveryError || errors.discoveryEndpoint
              ? "error"
              : !discoveryResult
              ? "default"
              : "success"
          }
          helperTextInvalid={
            errors.discoveryEndpoint
              ? t("common:required")
              : t("noValidMetaDataFound", {
                  error: errors.discoveryError?.message,
                })
          }
          isRequired
        >
          <KeycloakTextInput
            type="url"
            data-testid="discoveryEndpoint"
            id="kc-discovery-endpoint"
            placeholder={
              id === "oidc"
                ? "https://hostname/auth/realms/master/.well-known/openid-configuration"
                : ""
            }
            validated={
              errors.discoveryError || errors.discoveryEndpoint
                ? "error"
                : !discoveryResult
                ? "default"
                : "success"
            }
            customIconUrl={
              discovering
                ? environment.resourceUrl + "/discovery-load-indicator.svg"
                : ""
            }
            {...register("discoveryEndpoint", {
              required: true,
              onBlur: () => setDiscovering(true),
            })}
          />
        </FormGroup>
      )}
      {!discovery && fileUpload}
      {discovery && !errors.discoveryError && children(true)}
      {!discovery && children(false)}
    </>
  );
};
