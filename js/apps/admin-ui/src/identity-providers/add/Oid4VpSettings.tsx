import { TextControl } from "@keycloak/keycloak-ui-shared";
import { IdentityProviderType } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { IdentityProviderSelect } from "../../components/identity-provider/IdentityProviderSelect";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import type { IdentityProviderParams } from "../routes/IdentityProvider";

const PROVIDER_ID = "oid4vp";
const TRUST_MATERIAL_IDPS = "trustMaterialIdps";

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

  const trustMaterialIdps = properties.find(
    ({ name }) => name === TRUST_MATERIAL_IDPS,
  );
  const trustMaterialIndex = trustMaterialIdps
    ? properties.indexOf(trustMaterialIdps)
    : properties.length;

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
      <DynamicComponents
        stringify
        properties={properties.slice(0, trustMaterialIndex)}
      />
      {trustMaterialIdps && (
        <IdentityProviderSelect
          name={`config.${TRUST_MATERIAL_IDPS}`}
          label={trustMaterialIdps.label}
          helpText={trustMaterialIdps.helpText}
          convertToName={(name) => name}
          identityProviderType={IdentityProviderType.TRUST_MATERIAL}
          realmOnly
          stringify
          stringifySeparator=","
        />
      )}
      <DynamicComponents
        stringify
        properties={properties.slice(trustMaterialIndex + 1)}
      />
    </>
  );
}
