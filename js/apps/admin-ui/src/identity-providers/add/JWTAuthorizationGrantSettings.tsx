import { useTranslation } from "react-i18next";

import { TextControl, NumberControl } from "@keycloak/keycloak-ui-shared";
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
      <JWTAuthorizationGrantAssertionSettings />
      <NumberControl
        name="config.jwtAuthorizationGrantAllowedClockSkew"
        label={t("allowedClockSkew")}
        labelIcon={t("allowedClockSkewHelp")}
        controller={{ defaultValue: 0, rules: { min: 0, max: 2147483 } }}
      />
      <Divider />
    </>
  );
}
