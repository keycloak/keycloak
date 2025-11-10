import { useTranslation } from "react-i18next";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { Divider, FormGroup } from "@patternfly/react-core";
import { useWatch, useFormContext, Controller } from "react-hook-form";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import {
  SelectControl,
  HelpItem,
  NumberControl,
} from "@keycloak/keycloak-ui-shared";
import { sortProviders } from "../../util";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

type JWTAuthorizationGrantAssertionSettingsProps = {
  alwaysEnabled?: boolean;
};

export const JWTAuthorizationGrantAssertionSettings = ({
  alwaysEnabled = false,
}: JWTAuthorizationGrantAssertionSettingsProps) => {
  const { t } = useTranslation();
  const providers = useServerInfo().providers!.signature.providers;
  const { control } = useFormContext();
  const authorizationGrantSwitchEnabled = useWatch({
    control,
    name: "config.jwtAuthorizationGrantEnabled",
  });
  const isEnabled = alwaysEnabled || authorizationGrantSwitchEnabled === "true";

  return (
    <>
      {!alwaysEnabled && (
        <DefaultSwitchControl
          name="config.jwtAuthorizationGrantEnabled"
          label={t("jwtAuthorizationGrantIdpEnabled")}
          labelIcon={t("jwtAuthorizationGrantIdpEnabledHelp")}
          stringify
        />
      )}

      {isEnabled && (
        <>
          <DefaultSwitchControl
            name="config.jwtAuthorizationGrantAssertionReuseAllowed"
            label={t("jwtAuthorizationGrantAssertionReuseAllowed")}
            labelIcon={t("jwtAuthorizationGrantAssertionReuseAllowedHelp")}
            stringify
          />

          <FormGroup
            label={t("jwtAuthorizationGrantMaxAllowedAssertionExpiration")}
            fieldId="jwtAuthorizationGrantMaxAllowedAssertionExpiration"
            labelIcon={
              <HelpItem
                helpText={t(
                  "jwtAuthorizationGrantMaxAllowedAssertionExpirationHelp",
                )}
                fieldLabelId="jwtAuthorizationGrantMaxAllowedAssertionExpirationHelp"
              />
            }
          >
            <Controller
              name="config.jwtAuthorizationGrantMaxAllowedAssertionExpirationHelp"
              defaultValue={300}
              control={control}
              render={({ field }) => (
                <TimeSelector
                  data-testid="jwtAuthorizationGrantMaxAllowedAssertionExpirationHelp"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["second", "minute", "hour"]}
                />
              )}
            />
          </FormGroup>
          <SelectControl
            name="config.jwtAuthorizationGrantAssertionSignatureAlg"
            label={t("jwtAuthorizationGrantAssertionSignatureAlg")}
            labelIcon={t("jwtAuthorizationGrantAssertionSignatureAlgHelp")}
            options={[
              { key: "", value: t("algorithmNotSpecified") },
              ...sortProviders(providers).map((p) => ({ key: p, value: p })),
            ]}
            controller={{
              defaultValue: "",
            }}
          />
          <NumberControl
            name="config.jwtAuthorizationGrantAllowedClockSkew"
            label={t("allowedClockSkew")}
            labelIcon={t("allowedClockSkewHelp")}
            controller={{ defaultValue: 0, rules: { min: 0, max: 2147483 } }}
          />
        </>
      )}
      <Divider />
    </>
  );
};
