import { useTranslation } from "react-i18next";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { Divider } from "@patternfly/react-core";
import { useWatch, useFormContext } from "react-hook-form";

export const JwtAuthorizationGrantSettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const authorizationGrantEnabled = useWatch({
    control,
    name: "config.jwtAuthorizationGrantEnabled",
  });
  return (
    <>
      <DefaultSwitchControl
        name="config.jwtAuthorizationGrantEnabled"
        label={t("jwtAuthorizationGrantIdpEnabled")}
        labelIcon={t("jwtAuthorizationGrantIdpEnabledHelp")}
        stringify
      />
      {authorizationGrantEnabled === "true" && (
        <DefaultSwitchControl
          name="config.jwtAuthorizationGrantAssertionReuseAllowed"
          label={t("jwtAuthorizationGrantAssertionReuseAllowed")}
          labelIcon={t("jwtAuthorizationGrantAssertionReuseAllowedHelp")}
          stringify
        />
      )}
      <Divider />
    </>
  );
};
