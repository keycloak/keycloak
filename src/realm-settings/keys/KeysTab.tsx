import { useState } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Tab, TabTitleText } from "@patternfly/react-core";

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { KEY_PROVIDER_TYPE } from "../../util";
import {
  routableTab,
  RoutableTabs,
} from "../../components/routable-tabs/RoutableTabs";
import { KeySubTab, toKeysTab } from "../routes/KeysTab";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { KeysListTab } from "./KeysListTab";
import { KeysProvidersTab } from "./KeysProvidersTab";

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

export const KeysTab = () => {
  const { t } = useTranslation("realm-settings");
  const history = useHistory();

  const { adminClient } = useAdminClient();
  const { realm: realmName } = useRealm();

  const [realmComponents, setRealmComponents] =
    useState<ComponentRepresentation[]>();
  const [key, setKey] = useState(0);
  const refresh = () => {
    setKey(key + 1);
  };

  useFetch(
    () =>
      adminClient.components.find({
        type: KEY_PROVIDER_TYPE,
        realm: realmName,
      }),
    (components) => setRealmComponents(sortByPriority(components)),
    [key]
  );

  if (!realmComponents) {
    return <KeycloakSpinner />;
  }

  const keysRoute = (tab: KeySubTab) =>
    routableTab({
      to: toKeysTab({ realm: realmName, tab }),
      history,
    });

  return (
    <RoutableTabs
      mountOnEnter
      unmountOnExit
      defaultLocation={toKeysTab({ realm: realmName, tab: "list" })}
    >
      <Tab
        id="keysList"
        data-testid="rs-keys-list-tab"
        aria-label="keys-list-subtab"
        title={<TabTitleText>{t("keysList")}</TabTitleText>}
        {...keysRoute("list")}
      >
        <KeysListTab realmComponents={realmComponents} />
      </Tab>
      <Tab
        id="providers"
        data-testid="rs-providers-tab"
        aria-label="rs-providers-tab"
        title={<TabTitleText>{t("providers")}</TabTitleText>}
        {...keysRoute("providers")}
      >
        <KeysProvidersTab realmComponents={realmComponents} refresh={refresh} />
      </Tab>
    </RoutableTabs>
  );
};
