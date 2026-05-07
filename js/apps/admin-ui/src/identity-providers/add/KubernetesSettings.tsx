import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { FormGroupField } from "../component/FormGroupField";
import { Controller, useFormContext } from "react-hook-form";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import IdentityProviderRepresentation from "libs/keycloak-admin-client/lib/defs/identityProviderRepresentation";

export const DEFAULT_KUBERNETES_ISSUER =
  "https://kubernetes.default.svc.cluster.local";

export const KubernetesSettings = () => {
  const { t } = useTranslation();
  const { control, watch } = useFormContext<IdentityProviderRepresentation>();
  const issuerUrl = watch("config.issuer");
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
        label={t("kubernetesIssuerUrl")}
        labelIcon={t("kubernetesIssuerUrlHelp")}
        isHelpIconWarning={issuerUrl === DEFAULT_KUBERNETES_ISSUER}
        rules={{
          required: t("required"),
        }}
      />
      <FormGroupField label="kubernetesFedClientAssertionMaxExp">
        <Controller
          name="config.fedClientAssertionMaxExp"
          defaultValue="3600"
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
