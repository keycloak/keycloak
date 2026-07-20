import { useTranslation } from "react-i18next";
import { TextAreaControl, TextControl } from "@keycloak/keycloak-ui-shared";
import { JwksSettings } from "./JwksSettings";
import { useParams } from "react-router-dom";
import type { IdentityProviderParams } from "../routes/IdentityProvider";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useFormContext, useWatch } from "react-hook-form";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";

export default function DefaultTrustSettings() {
  const { t } = useTranslation();
  const { tab } = useParams<IdentityProviderParams>();
  const { control } = useFormContext<IdentityProviderRepresentation>();
  const useX509 = useWatch({
    control,
    name: "config.useX509",
    defaultValue: "false",
  });

  return (
    <>
      <TextControl
        name="alias"
        label={t("alias")}
        labelIcon={t("aliasHelp")}
        readOnly={tab === "settings"}
        rules={{
          required: t("required"),
        }}
      />
      <DefaultSwitchControl
        name="config.useX509"
        label={t("useX509AttestationTrust")}
        labelIcon={t("useX509AttestationTrustHelp")}
        defaultValue="false"
        stringify
      />
      {useX509 === "true" ? (
        <>
          <TextAreaControl
            name="config.trustedCertificates"
            label={t("trustedAttestationCertificates")}
            labelIcon={t("trustedAttestationCertificatesHelp")}
            rules={{ required: t("required") }}
          />
          <TextControl
            name="config.attestationExtendedKeyUsages"
            label={t("attestationExtendedKeyUsages")}
            labelIcon={t("attestationExtendedKeyUsagesHelp")}
            rules={{ required: t("required") }}
          />
          <DefaultSwitchControl
            name="config.certificateRevocationEnabled"
            label={t("certificateRevocation")}
            labelIcon={t("certificateRevocationHelp")}
            defaultValue="true"
            stringify
          />
        </>
      ) : (
        <JwksSettings />
      )}
    </>
  );
}
