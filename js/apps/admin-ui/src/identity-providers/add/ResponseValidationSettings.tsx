import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { sortProviders } from "../../util";

export const ResponseValidationSettings = ({ isOIDC }: { isOIDC: boolean }) => {
  const { t } = useTranslation();
  const providers = useServerInfo().providers!.clientSignature.providers;

  return (
    <>
      <DefaultSwitchControl
        name="config.authorizationResponseIssuerRequired"
        label={t("authorizationResponseIssuerRequired")}
        labelIcon={t("authorizationResponseIssuerRequiredHelp")}
        stringify
      />
      {isOIDC && (
        <>
          <SelectControl
            name="config.idTokenSigningAlg"
            label={t("idTokenSigningAlg")}
            labelIcon={t("idTokenSigningAlgHelp")}
            controller={{
              defaultValue: "",
            }}
            options={[
              { key: "", value: t("algorithmNotSpecified") },
              ...sortProviders(providers).map((p) => ({ key: p, value: p })),
            ]}
          />
          <DefaultSwitchControl
            name="config.rejectOtherIdTokenAudiences"
            label={t("rejectOtherIdTokenAudiences")}
            labelIcon={t("rejectOtherIdTokenAudiencesHelp")}
            stringify
          />
        </>
      )}
    </>
  );
};
