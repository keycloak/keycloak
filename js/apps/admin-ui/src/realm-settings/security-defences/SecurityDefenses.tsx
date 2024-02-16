import { useState } from "react";
import { useTranslation } from "react-i18next";
import { PageSection, Tab, Tabs, TabTitleText } from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { HeadersForm } from "./HeadersForm";
import { BruteForceDetection } from "./BruteForceDetection";

type SecurityDefensesProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const SecurityDefenses = ({ realm, save }: SecurityDefensesProps) => {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState(10);
  return (
    <Tabs
      activeKey={activeTab}
      onSelect={(_, key) => setActiveTab(key as number)}
    >
      <Tab
        id="headers"
        eventKey={10}
        data-testid="security-defenses-headers-tab"
        title={<TabTitleText>{t("headers")}</TabTitleText>}
      >
        <PageSection variant="light">
          <HeadersForm realm={realm} save={save} />
        </PageSection>
      </Tab>
      <Tab
        id="bruteForce"
        eventKey={20}
        data-testid="security-defenses-brute-force-tab"
        title={<TabTitleText>{t("bruteForceDetection")}</TabTitleText>}
      >
        <PageSection variant="light">
          <BruteForceDetection realm={realm} save={save} />
        </PageSection>
      </Tab>
    </Tabs>
  );
};
