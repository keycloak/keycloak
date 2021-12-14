import React, { ReactNode, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, TextInput, Switch } from "@patternfly/react-core";

import environment from "../../environment";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useAdminClient } from "../../context/auth/AdminClient";

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

  const adminClient = useAdminClient();

  const { setValue, register, errors, setError, watch, clearErrors } =
    useFormContext();
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
        label={t("useDiscoveryEndpoint")}
        fieldId="kc-discovery-endpoint-switch"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:useDiscoveryEndpoint"
            fieldLabelId="identity-providers:discoveryEndpoint"
          />
        }
      >
        <Switch
          id="kc-discovery-endpoint-switch"
          label={t("common:on")}
          labelOff={t("common:off")}
          isChecked={discovery}
          onChange={setDiscovery}
        />
      </FormGroup>
      {discovery && (
        <FormGroup
          label={t("discoveryEndpoint")}
          fieldId="kc-discovery-endpoint"
          labelIcon={
            <HelpItem
              helpText="identity-providers-help:discoveryEndpoint"
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
              : t("noValidMetaDataFound")
          }
          isRequired
        >
          <TextInput
            type="text"
            name="discoveryEndpoint"
            data-testid="discoveryEndpoint"
            id="kc-discovery-endpoint"
            placeholder={
              id === "oidc"
                ? "https://hostname/auth/realms/master/.well-known/openid-configuration"
                : "https://hostname/context/saml/discovery"
            }
            onBlur={() => setDiscovering(true)}
            validated={
              errors.discoveryError || errors.discoveryEndpoint
                ? "error"
                : !discoveryResult
                ? "default"
                : "success"
            }
            customIconUrl={
              discovering
                ? environment.resourceUrl + "./discovery-load-indicator.svg"
                : ""
            }
            ref={register({ required: true })}
          />
        </FormGroup>
      )}
      {!discovery && fileUpload}
      {discovery && !errors.discoveryError && children(true)}
      {!discovery && children(false)}
    </>
  );
};
