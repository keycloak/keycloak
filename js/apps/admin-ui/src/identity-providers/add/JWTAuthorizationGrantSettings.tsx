import { useTranslation } from "react-i18next";

import { TextControl } from "@keycloak/keycloak-ui-shared";
import { JWTAuthorizationGrantAssertionSettings } from "./JWTAuthorizationGrantAssertionSettings";
import { Divider } from "@patternfly/react-core";
export default function JWTAuthorizationGrantSettings() {
  const { t } = useTranslation();
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
        label={t("issuer")}
        rules={{
          required: t("required"),
        }}
      />
      <TextControl
        name="config.jwksUrl"
        label={t("jwtAuthorizationGrantJWKSUrl")}
        labelIcon={t("jwtAuthorizationGrantJWKSUrlHelp")}
        rules={{
          required: t("required"),
        }}
      />
      <Divider />
      <JWTAuthorizationGrantAssertionSettings alwaysEnabled={true} />
    </>
  );
}
