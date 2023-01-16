import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Tab, Tabs, TabTitleText } from "@patternfly/react-core";

import { ResourcesTab } from "./ResourcesTab";
import { Page } from "../components/page/Page";

const Resources = () => {
  const { t } = useTranslation();
  const [activeTabKey, setActiveTabKey] = useState(0);

  return (
    <Page title={t("resources")} description={t("resourceIntroMessage")}>
      <Tabs
        activeKey={activeTabKey}
        onSelect={(_, key) => setActiveTabKey(key as number)}
        mountOnEnter
        unmountOnExit
      >
        <Tab
          eventKey={0}
          title={<TabTitleText>{t("myResources")}</TabTitleText>}
        >
          <ResourcesTab />
        </Tab>
        <Tab
          eventKey={1}
          title={<TabTitleText>{t("sharedWithMe")}</TabTitleText>}
        >
          <h1>Share</h1>
        </Tab>
      </Tabs>
    </Page>
  );
};

export default Resources;
