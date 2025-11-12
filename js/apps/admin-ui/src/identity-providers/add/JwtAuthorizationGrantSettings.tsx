import { useTranslation } from "react-i18next";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { Divider, FormGroup } from "@patternfly/react-core";
import { useWatch, useFormContext, Controller } from "react-hook-form";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { SelectControl, HelpItem } from "@keycloak/keycloak-ui-shared";
import { sortProviders } from "../../util";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

export const JwtAuthorizationGrantSettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const authorizationGrantEnabled = useWatch({
    control,
    name: "config.jwtAuthorizationGrantEnabled",
  });
  const providers = useServerInfo().providers!.signature.providers;
  return (
    <>
      <DefaultSwitchControl
        name="config.jwtAuthorizationGrantEnabled"
        label={t("jwtAuthorizationGrantIdpEnabled")}
        labelIcon={t("jwtAuthorizationGrantIdpEnabledHelp")}
        stringify
      />
      {authorizationGrantEnabled === "true" && (
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
        </>
      )}
      <Divider />
    </>
  );
};
