import { Tab, Tabs, TabTitleText } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { useRealm } from "../../context/realm-context/RealmContext";
import { CibaPolicy } from "./CibaPolicy";
import { OtpPolicy } from "./OtpPolicy";
import { PasswordPolicy } from "./PasswordPolicy";
import { WebauthnPolicy } from "./WebauthnPolicy";

export const PASSWORD_POLICY = "passwordPolicy";
export const OTP_POLICY = "otpPolicy";
export const WEBAUTHN_POLICY = "webauthnPolicy";
export const WEBAUTHN_PASSWORDLESS_POLICY = "webauthnPasswordlessPolicy";
export const CIBA_POLICY = "cibaPolicy";

const DEFAULT_TAB = PASSWORD_POLICY;

export const Policies = () => {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") || DEFAULT_TAB;
  const [subTab, setSubTab] = useState(initialTab);
  const { realmRepresentation: realm, refresh } = useRealm();

  return (
    <Tabs
      activeKey={subTab}
      onSelect={(_, key) => setSubTab(key as string)}
      mountOnEnter
      unmountOnExit
    >
      <Tab
        id={PASSWORD_POLICY}
        data-testid={PASSWORD_POLICY}
        eventKey={PASSWORD_POLICY}
        title={<TabTitleText>{t(PASSWORD_POLICY)}</TabTitleText>}
      >
        <PasswordPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
      <Tab
        id={OTP_POLICY}
        data-testid={OTP_POLICY}
        eventKey={OTP_POLICY}
        title={<TabTitleText>{t(OTP_POLICY)}</TabTitleText>}
      >
        <OtpPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
      <Tab
        id={WEBAUTHN_POLICY}
        data-testid={WEBAUTHN_POLICY}
        eventKey={WEBAUTHN_POLICY}
        title={<TabTitleText>{t(WEBAUTHN_POLICY)}</TabTitleText>}
      >
        <WebauthnPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
      <Tab
        id={WEBAUTHN_PASSWORDLESS_POLICY}
        data-testid={WEBAUTHN_PASSWORDLESS_POLICY}
        eventKey={WEBAUTHN_PASSWORDLESS_POLICY}
        title={<TabTitleText>{t(WEBAUTHN_PASSWORDLESS_POLICY)}</TabTitleText>}
      >
        <WebauthnPolicy realm={realm} realmUpdated={refresh} isPasswordLess />
      </Tab>
      <Tab
        id={CIBA_POLICY}
        data-testid={CIBA_POLICY}
        eventKey={CIBA_POLICY}
        title={<TabTitleText>{t(CIBA_POLICY)}</TabTitleText>}
      >
        <CibaPolicy realm={realm} realmUpdated={refresh} />
      </Tab>
    </Tabs>
  );
};
