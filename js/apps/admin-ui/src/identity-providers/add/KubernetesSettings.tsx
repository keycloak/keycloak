import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { FormGroupField } from "../component/FormGroupField";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import IdentityProviderRepresentation from "libs/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { DefaultSwitchControl } from "../../components/SwitchControl";

const DEFAULT_KUBERNETES_ISSUER_URL =
  "https://kubernetes.default.svc.cluster.local";

export const KubernetesSettings = () => {
  const { t } = useTranslation();
  const { control, setValue } =
    useFormContext<IdentityProviderRepresentation>();
  const automaticIssuerDiscovery = useWatch({
    control,
    name: "config.automaticIssuerDiscovery",
  });

  useEffect(() => {
    if (automaticIssuerDiscovery === "true") {
      setValue("config.issuerDiscoveryUrl", DEFAULT_KUBERNETES_ISSUER_URL, {
        shouldDirty: false,
      });
    } else {
      setValue("config.issuerDiscoveryUrl", "", { shouldDirty: false });
    }
  }, [automaticIssuerDiscovery, setValue]);

  return (
    <>
      <TextControl
        name="alias"
        label={t("alias")}
        labelIcon={t("aliasHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <DefaultSwitchControl
        name="config.automaticIssuerDiscovery"
        label={t("automaticIssuerDiscovery")}
        labelIcon={t("automaticIssuerDiscoveryHelp")}
        stringify
      />

      <TextControl
        name="config.issuer"
        labelIcon={t("kubernetesIssuerUrlHelp")}
        label={t("kubernetesIssuerUrl")}
        isDisabled={automaticIssuerDiscovery === "true"}
      />
      <FormGroupField label="fedClientAssertionMaxExp">
        <Controller
          name="config.fedClientAssertionMaxExp"
          defaultValue={""}
          control={control}
          render={({ field }) => (
            <TimeSelector
              className="kc-fed-client-assertion-max-expiration-time"
              data-testid="fed-client-assertion-max-expiration-time-input"
              value={field.value!}
              onChange={field.onChange}
              units={["minute", "hour", "day"]}
            />
          )}
        />
      </FormGroupField>
    </>
  );
};
