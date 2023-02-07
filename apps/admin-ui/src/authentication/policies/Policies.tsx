import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { Tab, Tabs, TabTitleText } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { CibaPolicy } from "./CibaPolicy";
import { OtpPolicy } from "./OtpPolicy";
import { PasswordPolicy } from "./PasswordPolicy";
import { WebauthnPolicy } from "./WebauthnPolicy";

export const Policies = () => {
  const { t } = useTranslation("authentication");
  const [subTab, setSubTab] = useState(1);
  const { adminClient } = useAdminClient();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();

  useFetch(
    async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      if (!realm) {
        throw new Error(t("common:notFound"));
      }
      return realm;
    },
    (realm) => {
      setRealm(realm);
    },
    []
  );

  if (!realm) {
    return <KeycloakSpinner />;
  }

  return (
    <Tabs
      activeKey={subTab}
      onSelect={(_, key) => setSubTab(key as number)}
      mountOnEnter
      unmountOnExit
    >
      <Tab
        id="passwordPolicy"
        eventKey={1}
        title={<TabTitleText>{t("passwordPolicy")}</TabTitleText>}
      >
        <PasswordPolicy realm={realm} realmUpdated={setRealm} />
      </Tab>
      <Tab
        id="otpPolicy"
        eventKey={2}
        title={<TabTitleText>{t("otpPolicy")}</TabTitleText>}
      >
        <OtpPolicy realm={realm} realmUpdated={setRealm} />
      </Tab>
      <Tab
        id="webauthnPolicy"
        eventKey={3}
        title={<TabTitleText>{t("webauthnPolicy")}</TabTitleText>}
      >
        <WebauthnPolicy realm={realm} realmUpdated={setRealm} />
      </Tab>
      <Tab
        id="webauthnPasswordlessPolicy"
        eventKey={4}
        title={<TabTitleText>{t("webauthnPasswordlessPolicy")}</TabTitleText>}
      >
        <WebauthnPolicy realm={realm} realmUpdated={setRealm} isPasswordLess />
      </Tab>
      <Tab
        data-testid="tab-ciba-policy"
        eventKey={5}
        title={<TabTitleText>{t("cibaPolicy")}</TabTitleText>}
      >
        <CibaPolicy realm={realm} realmUpdated={setRealm} />
      </Tab>
    </Tabs>
  );
};
