import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { AlertVariant, Tab, Tabs, TabTitleText } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { RoleMapping } from "../components/role-mapping/RoleMapping";
import { useRealm } from "../context/realm-context/RealmContext";
import { useFetch } from "../utils/useFetch";
import { DefaultsGroupsTab } from "./DefaultGroupsTab";

export const UserRegistration = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [activeTab, setActiveTab] = useState(10);
  const [key, setKey] = useState(0);

  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    setRealm,
    [],
  );

  if (!realm) {
    return <KeycloakSpinner />;
  }

  const addComposites = async (composites: RoleRepresentation[]) => {
    const compositeArray = composites;

    try {
      await adminClient.roles.createComposite(
        { roleId: realm.defaultRole!.id!, realm: realmName },
        compositeArray,
      );
      setKey(key + 1);
      addAlert(t("addAssociatedRolesSuccess"), AlertVariant.success);
    } catch (error) {
      addError("addAssociatedRolesError", error);
    }
  };

  return (
    <Tabs
      activeKey={activeTab}
      onSelect={(_, key) => setActiveTab(key as number)}
    >
      <Tab
        key={key}
        id="roles"
        eventKey={10}
        title={<TabTitleText>{t("defaultRoles")}</TabTitleText>}
        data-testid="default-roles-tab"
      >
        <RoleMapping
          name={realm.defaultRole!.name!}
          id={realm.defaultRole!.id!}
          type="roles"
          isManager
          save={(rows) => addComposites(rows.map((r) => r.role))}
        />
      </Tab>
      <Tab
        id="groups"
        eventKey={20}
        title={<TabTitleText>{t("defaultGroups")}</TabTitleText>}
        data-testid="default-groups-tab"
      >
        <DefaultsGroupsTab />
      </Tab>
    </Tabs>
  );
};
