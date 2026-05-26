import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { JwksSettings } from "./JwksSettings";
import { useParams } from "react-router-dom";
import type { IdentityProviderParams } from "../routes/IdentityProvider";
import { useEffect } from "react";
import { useFormContext } from "react-hook-form";

export default function DefaultTrustSettings() {
  const { t } = useTranslation();
  const { tab } = useParams<IdentityProviderParams>();
  const { getValues, setValue } = useFormContext();

  useEffect(() => {
    const config = getValues("config");

    if (config?.trustedJwksUrl && !config.jwksUrl) {
      setValue("config.jwksUrl", config.trustedJwksUrl);
      setValue("config.useJwksUrl", "true");
    } else if (config?.trustedJwks && !config.publicKeySignatureVerifier) {
      setValue("config.publicKeySignatureVerifier", config.trustedJwks);
      setValue("config.useJwksUrl", "false");
    } else if (config?.useJwksUrl === undefined) {
      setValue(
        "config.useJwksUrl",
        config?.publicKeySignatureVerifier ? "false" : "true",
      );
    }
  }, [getValues, setValue]);

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
      <JwksSettings
        publicKeyLabel="jwks"
        publicKeyHelp="jwksHelp"
        showPublicKeyId={false}
        allowImport={false}
      />
    </>
  );
}
