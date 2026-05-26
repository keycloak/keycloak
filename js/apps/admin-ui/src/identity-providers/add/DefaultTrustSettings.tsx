import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { JwksSettings } from "./JwksSettings";
import { useParams } from "react-router-dom";
import type { IdentityProviderParams } from "../routes/IdentityProvider";

export default function DefaultTrustSettings() {
  const { t } = useTranslation();
  const { tab } = useParams<IdentityProviderParams>();

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
      <JwksSettings />
    </>
  );
}
