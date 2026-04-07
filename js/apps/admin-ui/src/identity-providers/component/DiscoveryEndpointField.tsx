import { FormGroup, Spinner, Switch } from "@patternfly/react-core";
import debouncePromise from "p-debounce";
import { ReactNode, useCallback, useMemo, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import {
  SyncMultiselect,
  SYNCED_FIELDS,
  type SyncedField,
} from "../add/DiscoverySettings";

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
    control,
    formState: { errors },
  } = useFormContext();

  const [discovery, setDiscovery] = useState(true);
  const [discovering, setDiscovering] = useState(false);
  const [discoveryResult, setDiscoveryResult] =
    useState<Record<string, string>>();

  const reloadEnabled = useWatch({ control, name: "config.reloadEnabled" });
  const includedRaw = useWatch({
    control,
    name: "config.includedWellKnownFields",
  });
  const includedFields = includedRaw
    ? (includedRaw as string)
        .split("##")
        .filter(Boolean)
        .filter((f): f is SyncedField =>
          (SYNCED_FIELDS as readonly string[]).includes(f),
        )
    : [];

  const setupForm = (result: Record<string, string>) => {
    Object.keys(result).map((k) => setValue(`config.${k}`, result[k]));
  };

  const discover = useCallback(
    async (fromUrl: string) => {
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
    },
    [adminClient, id, setValue],
  );

  const discoverDebounced = useMemo(
    () => debouncePromise(discover, 1000),
    [discover],
  );

  const isOidc = id === "oidc";

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

          {isOidc && discoveryResult && (
            <DefaultSwitchControl
              name="config.reloadEnabled"
              label={t("reloadEnabled")}
              labelIcon={t("reloadEnabledHelp")}
              stringify
              onChange={(_e, checked) => {
                if (checked && !includedRaw) {
                  setValue(
                    "config.includedWellKnownFields",
                    SYNCED_FIELDS.join("##"),
                  );
                }
              }}
            />
          )}

          {isOidc && reloadEnabled === "true" && (
            <SyncMultiselect
              id="sync-these-fields"
              selected={includedFields}
              onSelect={(field) => {
                const next = includedFields.includes(field)
                  ? includedFields.filter((f) => f !== field)
                  : [...includedFields, field];
                setValue("config.includedWellKnownFields", next.join("##"));
              }}
            />
          )}
        </>
      )}
      {!discovery && fileUpload}
      {discovery &&
        !errors.discoveryError &&
        children(reloadEnabled !== "true")}
      {!discovery && children(false)}
    </>
  );
};
