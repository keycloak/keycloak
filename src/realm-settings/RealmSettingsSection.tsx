import { Breadcrumb, BreadcrumbItem, Spinner } from "@patternfly/react-core";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { KEY_PROVIDER_TYPE } from "../util";
import { toRealmSettings } from "./routes/RealmSettings";
import { RealmSettingsTabs } from "./RealmSettingsTabs";

export const EditProviderCrumb = () => {
  const { t } = useTranslation("realm-settings");
  const { realm } = useRealm();

  return (
    <Breadcrumb>
      <BreadcrumbItem
        render={(props) => (
          <Link {...props} to={toRealmSettings({ realm, tab: "keys" })}>
            {t("keys")}
          </Link>
        )}
      />
      <BreadcrumbItem>{t("providers")}</BreadcrumbItem>
      <BreadcrumbItem isActive>{t("editProvider")}</BreadcrumbItem>
    </Breadcrumb>
  );
};

const sortByPriority = (components: ComponentRepresentation[]) => {
  const sortedComponents = [...components].sort((a, b) => {
    const priorityA = Number(a.config?.priority);
    const priorityB = Number(b.config?.priority);

    return (
      (!isNaN(priorityB) ? priorityB : 0) - (!isNaN(priorityA) ? priorityA : 0)
    );
  });

  return sortedComponents;
};

export const RealmSettingsSection = () => {
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [realmComponents, setRealmComponents] =
    useState<ComponentRepresentation[]>();
  const [currentUser, setCurrentUser] = useState<UserRepresentation>();
  const { whoAmI } = useWhoAmI();
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(key + 1);
  };

  // delays realm fetch by 100ms in order to fetch newly updated value from server
  useEffect(() => {
    const update = async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      setRealm(realm);
    };
    setTimeout(update, 100);
  }, [key]);

  useFetch(
    async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      const realmComponents = await adminClient.components.find({
        type: KEY_PROVIDER_TYPE,
        realm: realmName,
      });
      const user = await adminClient.users.findOne({ id: whoAmI.getUserId() });

      return { user, realm, realmComponents };
    },
    ({ user, realm, realmComponents }) => {
      setRealmComponents(sortByPriority(realmComponents));
      setCurrentUser(user);
      setRealm(realm);
    },
    []
  );

  if (!realm || !realmComponents || !currentUser) {
    return (
      <div className="pf-u-text-align-center">
        <Spinner />
      </div>
    );
  }
  return (
    <RealmSettingsTabs
      realm={realm}
      refresh={refresh}
      realmComponents={realmComponents}
      currentUser={currentUser}
    />
  );
};
