import { TextControl } from "@keycloak/keycloak-ui-shared";
import { Radio } from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { FormGroupField } from "../component/FormGroupField";
import { Controller, useFormContext } from "react-hook-form";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import IdentityProviderRepresentation from "libs/keycloak-admin-client/lib/defs/identityProviderRepresentation";

const DEFAULT_KUBERNETES_API_SERVER_URL = "https://kubernetes.default.svc";

type DiscoveryMode = "inCluster" | "external";

const discoveryMode = (issuerDiscoveryUrl?: string): DiscoveryMode =>
  !issuerDiscoveryUrl ||
  issuerDiscoveryUrl === DEFAULT_KUBERNETES_API_SERVER_URL
    ? "inCluster"
    : "external";

export const KubernetesSettings = () => {
  const { t } = useTranslation();
  const { control, getValues, setValue } =
    useFormContext<IdentityProviderRepresentation>();
  const [mode, setMode] = useState<DiscoveryMode>(() =>
    discoveryMode(getValues("config.issuerDiscoveryUrl")),
  );

  useEffect(() => {
    if (!getValues("config.automaticIssuerDiscovery")) {
      setValue("config.automaticIssuerDiscovery", "true", {
        shouldDirty: false,
      });
    }
    if (mode === "inCluster" && !getValues("config.issuerDiscoveryUrl")) {
      setValue("config.issuerDiscoveryUrl", DEFAULT_KUBERNETES_API_SERVER_URL, {
        shouldDirty: false,
      });
    }
  }, [getValues, mode, setValue]);

  const setDiscoveryMode = (newMode: DiscoveryMode) => {
    setMode(newMode);
    setValue("config.automaticIssuerDiscovery", "true");
    if (newMode === "inCluster") {
      setValue("config.issuerDiscoveryUrl", DEFAULT_KUBERNETES_API_SERVER_URL);
    } else if (
      getValues("config.issuerDiscoveryUrl") ===
      DEFAULT_KUBERNETES_API_SERVER_URL
    ) {
      setValue("config.issuerDiscoveryUrl", "");
    }
  };

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
      <TextControl
        name="config.issuer"
        labelIcon={t("kubernetesIssuerUrlHelp")}
        label={t("kubernetesIssuerUrl")}
      />
      <FormGroupField label="kubernetesIssuerDiscoveryMode">
        <Radio
          id="kubernetes-in-cluster-discovery"
          data-testid="kubernetes-in-cluster-discovery"
          name="kubernetesIssuerDiscoveryMode"
          label={t("kubernetesInClusterDiscovery")}
          description={t("kubernetesInClusterDiscoveryHelp")}
          isChecked={mode === "inCluster"}
          onChange={() => setDiscoveryMode("inCluster")}
        />
        <Radio
          id="kubernetes-external-discovery"
          data-testid="kubernetes-external-discovery"
          name="kubernetesIssuerDiscoveryMode"
          label={t("kubernetesExternalDiscovery")}
          description={t("kubernetesExternalDiscoveryHelp")}
          isChecked={mode === "external"}
          onChange={() => setDiscoveryMode("external")}
        />
      </FormGroupField>
      <TextControl
        name="config.issuerDiscoveryUrl"
        labelIcon={t("kubernetesIssuerDiscoveryUrlHelp")}
        label={t("kubernetesIssuerDiscoveryUrl")}
        readOnly={mode === "inCluster"}
        rules={{
          required: mode === "external" ? t("required") : false,
        }}
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
