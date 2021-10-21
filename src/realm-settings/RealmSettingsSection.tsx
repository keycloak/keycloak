import { Breadcrumb, BreadcrumbItem, Spinner } from "@patternfly/react-core";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { KEY_PROVIDER_TYPE } from "../util";
import { toRealmSettings } from "./routes/RealmSettings";
import { RealmSettingsTabs } from "./RealmSettingsTabs";
import { toClientPolicies } from "./routes/ClientPolicies";

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

export const ToClientPolicies = () => {
  const { t } = useTranslation("realm-settings");
  const { realm } = useRealm();

  return (
    <BreadcrumbItem
      render={(props) => (
        <Link {...props} to={toClientPolicies({ realm })}>
          {t("clientPolicies")}
        </Link>
      )}
    />
  );
};

export const EditPolicyCrumb = () => {
  const { t } = useTranslation("realm-settings");

  return (
    <Breadcrumb>
      <ToClientPolicies />
      <BreadcrumbItem isActive>{t("policyDetails")}</BreadcrumbItem>
    </Breadcrumb>
  );
};

export const NewPolicyCrumb = () => {
  const { t } = useTranslation("realm-settings");
  const { realm } = useRealm();

  return (
    <Breadcrumb>
      <BreadcrumbItem
        render={(props) => (
          <Link {...props} to={toClientPolicies({ realm })}>
            {t("clientPolicies")}
          </Link>
        )}
      />
      <BreadcrumbItem isActive>{t("createPolicy")}</BreadcrumbItem>
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
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(key + 1);
  };

  useFetch(
    async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      const realmComponents = await adminClient.components.find({
        type: KEY_PROVIDER_TYPE,
        realm: realmName,
      });

      return { realm, realmComponents };
    },
    ({ realm, realmComponents }) => {
      setRealmComponents(sortByPriority(realmComponents));
      setRealm(realm);
    },
    [key]
  );

  if (!realm || !realmComponents) {
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
    />
  );
};
