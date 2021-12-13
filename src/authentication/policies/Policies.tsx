import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Tab, Tabs, TabTitleText } from "@patternfly/react-core";

import { PasswordPolicy } from "./PasswordPolicy";
import { OtpPolicy } from "./OtpPolicy";

export const Policies = () => {
  const { t } = useTranslation("authentication");
  const [subTab, setSubTab] = useState(1);
  return (
    <Tabs
      activeKey={subTab}
      onSelect={(_, key) => setSubTab(key as number)}
      mountOnEnter
    >
      <Tab
        id="passwordPolicy"
        eventKey={1}
        title={<TabTitleText>{t("passwordPolicy")}</TabTitleText>}
      >
        <PasswordPolicy />
      </Tab>
      <Tab
        id="otpPolicy"
        eventKey={2}
        title={<TabTitleText>{t("otpPolicy")}</TabTitleText>}
      >
        <OtpPolicy />
      </Tab>
      <Tab
        id="webauthnPolicy"
        eventKey={3}
        title={<TabTitleText>{t("webauthnPolicy")}</TabTitleText>}
      ></Tab>
      <Tab
        id="webauthnPasswordlessPolicy"
        eventKey={4}
        title={<TabTitleText>{t("webauthnPasswordlessPolicy")}</TabTitleText>}
      ></Tab>
    </Tabs>
  );
};
