import { Tab, Tabs, TabTitleText } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import { CibaPolicy } from "./CibaPolicy";
import { OtpPolicy } from "./OtpPolicy";
import { PasswordPolicy } from "./PasswordPolicy";
import { WebauthnPolicy } from "./WebauthnPolicy";

export const Policies = () => {
  const { t } = useTranslation();
  const [subTab, setSubTab] = useState(1);
  const { realmRepresentation: realm, refresh } = useRealm();

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
        data-testid="passwordPolicy"
        eventKey={1}
        title={<TabTitleText>{t("passwordPolicy")}</TabTitleText>}
      >
        <PasswordPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
      <Tab
        id="otpPolicy"
        data-testid="otpPolicy"
        eventKey={2}
        title={<TabTitleText>{t("otpPolicy")}</TabTitleText>}
      >
        <OtpPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
      <Tab
        id="webauthnPolicy"
        data-testid="webauthnPolicy"
        eventKey={3}
        title={<TabTitleText>{t("webauthnPolicy")}</TabTitleText>}
      >
        <WebauthnPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
      <Tab
        id="webauthnPasswordlessPolicy"
        data-testid="webauthnPasswordlessPolicy"
        eventKey={4}
        title={<TabTitleText>{t("webauthnPasswordlessPolicy")}</TabTitleText>}
      >
        <WebauthnPolicy realm={realm} realmUpdated={refresh} isPasswordLess />
      </Tab>
      <Tab
        data-testid="tab-ciba-policy"
        eventKey={5}
        title={<TabTitleText>{t("cibaPolicy")}</TabTitleText>}
      >
        <CibaPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
    </Tabs>
  );
};
