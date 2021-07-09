import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { PageSection, Tab, Tabs, TabTitleText } from "@patternfly/react-core";

import type RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { HeadersForm } from "./HeadersForm";
import { BruteForceDetection } from "./BruteForceDetection";

type SecurityDefencesProps = {
  save: (realm: RealmRepresentation) => void;
  reset: () => void;
};

export const SecurityDefences = ({ save, reset }: SecurityDefencesProps) => {
  const { t } = useTranslation("realm-settings");
  const [activeTab, setActiveTab] = useState(10);
  return (
    <Tabs
      activeKey={activeTab}
      onSelect={(_, key) => setActiveTab(key as number)}
    >
      <Tab
        id="headers"
        eventKey={10}
        title={<TabTitleText>{t("headers")}</TabTitleText>}
      >
        <PageSection variant="light">
          <HeadersForm save={save} reset={reset} />
        </PageSection>
      </Tab>
      <Tab
        id="bruteForce"
        eventKey={20}
        title={<TabTitleText>{t("bruteForceDetection")}</TabTitleText>}
      >
        <PageSection variant="light">
          <BruteForceDetection save={save} reset={reset} />
        </PageSection>
      </Tab>
    </Tabs>
  );
};
