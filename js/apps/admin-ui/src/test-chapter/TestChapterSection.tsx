import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";

import { toTestChapter } from "./routes/TestChapter";
import FirstTab from "./FirstTab";
import { SecondTab } from "./SecondTab";

export default function TestChapterSection() {
  const { t } = useTranslation();
  const { realm } = useRealm();

  const firstTab = useRoutableTab(toTestChapter({ realm, tab: "first-tab" }));
  const secondTab = useRoutableTab(toTestChapter({ realm, tab: "second-tab" }));

  return (
    <>
      <ViewHeader titleKey="Title of Test Chapter" divider={false} />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <RoutableTabs
          isBox
          defaultLocation={toTestChapter({ realm, tab: "first-tab" })}
        >
          <Tab
            title={<TabTitleText>{t("firstTab")}</TabTitleText>}
            data-testid="first-tab"
            {...firstTab}
          >
            <FirstTab />
          </Tab>
          <Tab
            title={<TabTitleText>{t("secondTab")}</TabTitleText>}
            data-testid="users-table-tab-link"
            {...secondTab}
          >
            <SecondTab />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
