import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { FormGroupField } from "../component/FormGroupField";
import IdentityProviderRepresentation from "libs/keycloak-admin-client/lib/defs/identityProviderRepresentation";

export const SpiffeSettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext<IdentityProviderRepresentation>();

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
        name="config.trustDomain"
        label={t("spiffeTrustDomain")}
        labelIcon={t("spiffeTrustDomainHelp")}
        rules={{
          required: t("required"),
        }}
      />

      <TextControl
        name="config.bundleEndpoint"
        label={t("spiffeBundleEndpoint")}
        labelIcon={t("Specify a URL starting with 'https://'.")}
        rules={{
          required: t("required"),
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
