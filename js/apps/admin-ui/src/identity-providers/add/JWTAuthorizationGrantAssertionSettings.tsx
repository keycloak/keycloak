import { useTranslation } from "react-i18next";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { FormGroup } from "@patternfly/react-core";
import { useFormContext, Controller } from "react-hook-form";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { SelectControl, HelpItem } from "@keycloak/keycloak-ui-shared";
import { sortProviders } from "../../util";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

export const JWTAuthorizationGrantAssertionSettings = () => {
  const { t } = useTranslation();
  const providers = useServerInfo().providers!.signature.providers;
  const { control } = useFormContext();
  return (
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
  );
};
