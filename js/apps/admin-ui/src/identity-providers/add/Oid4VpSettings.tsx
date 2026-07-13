import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import type { IdentityProviderParams } from "../routes/IdentityProvider";

const PROVIDER_ID = "oid4vp";

export default function Oid4VpSettings() {
  const { t } = useTranslation();
  const { tab } = useParams<IdentityProviderParams>();
  const serverInfo = useServerInfo();

  const properties = useMemo(
    () =>
      serverInfo.componentTypes?.[
        "org.keycloak.broker.provider.IdentityProvider"
      ]?.find(({ id }) => id === PROVIDER_ID)?.properties ?? [],
    [serverInfo],
  );

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
      <TextControl name="displayName" label={t("displayName")} />
      <DynamicComponents stringify properties={properties} />
    </>
  );
}
