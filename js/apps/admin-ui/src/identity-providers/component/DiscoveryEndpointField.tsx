import { FormGroup, Spinner, Switch } from "@patternfly/react-core";
import debouncePromise from "p-debounce";
import { ReactNode, useMemo, useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";

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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const {
    setValue,
    clearErrors,
    formState: { errors },
  } = useFormContext();
  const [discovery, setDiscovery] = useState(true);
  const [discovering, setDiscovering] = useState(false);
  const [discoveryResult, setDiscoveryResult] =
    useState<Record<string, string>>();

  const setupForm = (result: Record<string, string>) => {
    Object.keys(result).map((k) => setValue(`config.${k}`, result[k]));
  };

  const discover = async (fromUrl: string) => {
    setDiscovering(true);
    try {
      const result = await adminClient.identityProviders.importFromUrl({
        providerId: id,
        fromUrl,
      });
      setupForm(result);
      setDiscoveryResult(result);
    } catch (error) {
      return (error as Error).message;
    } finally {
      setDiscovering(false);
    }
  };

  const discoverDebounced = useMemo(() => debouncePromise(discover, 1000), []);

  return (
    <>
      <FormGroup
        label={t(
          id === "oidc" ? "useDiscoveryEndpoint" : "useEntityDescriptor",
        )}
        fieldId="kc-discovery-endpoint"
        labelIcon={
          <HelpItem
            helpText={t(
              id === "oidc"
                ? "useDiscoveryEndpointHelp"
                : "useEntityDescriptorHelp",
            )}
            fieldLabelId="discoveryEndpoint"
          />
        }
      >
        <Switch
          id="kc-discovery-endpoint-switch"
          label={t("on")}
          labelOff={t("off")}
          isChecked={discovery}
          onChange={(_event, checked) => {
            clearErrors("discoveryError");
            setDiscovery(checked);
          }}
          aria-label={t(
            id === "oidc" ? "useDiscoveryEndpoint" : "useEntityDescriptor",
          )}
        />
      </FormGroup>
      {discovery && (
        <>
          <div style={{ display: "none" }} data-testid="playwright-result">
            {errors.discoveryError || errors.discoveryEndpoint
              ? "error"
              : !discoveryResult
                ? "default"
                : "success"}
          </div>
          <TextControl
            name="discoveryEndpoint"
            label={t(
              id === "oidc" ? "discoveryEndpoint" : "samlEntityDescriptor",
            )}
            labelIcon={t(
              id === "oidc"
                ? "discoveryEndpointHelp"
                : "samlEntityDescriptorHelp",
            )}
            type="url"
            placeholder={
              id === "oidc"
                ? "https://hostname/realms/master/.well-known/openid-configuration"
                : ""
            }
            validated={
              errors.discoveryError || errors.discoveryEndpoint
                ? "error"
                : !discoveryResult
                  ? "default"
                  : "success"
            }
            customIcon={discovering ? <Spinner isInline /> : undefined}
            rules={{
              required: t("required"),
              validate: (value: string) => discoverDebounced(value),
            }}
          />
        </>
      )}
      {!discovery && fileUpload}
      {discovery && !errors.discoveryError && children(true)}
      {!discovery && children(false)}
    </>
  );
};
