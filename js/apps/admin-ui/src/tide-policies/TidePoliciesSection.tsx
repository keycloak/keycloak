/** TIDECLOAK IMPLEMENTATION */

import { useTranslation } from "react-i18next";
import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { ViewHeader } from "../components/view-header/ViewHeader";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import {
  type TidePoliciesTab,
  toTidePolicies,
} from "./routes/TidePolicies";
import { useRealm } from "../context/realm-context/RealmContext";
import { OverviewTab } from "./OverviewTab";

export default function TidePoliciesSection() {
  const { t } = useTranslation();
  const { realm } = useRealm();

  const useTab = (tab: TidePoliciesTab) =>
    useRoutableTab(toTidePolicies({ realm, tab }));

  const overviewTab = useTab("overview");

  return (
    <>
      <ViewHeader
        titleKey="Policies"
        subKey="View committed policies for this realm"
        divider={false}
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <RoutableTabs
          mountOnEnter
          isBox
          defaultLocation={toTidePolicies({ realm, tab: "overview" })}
        >
          <Tab
            title={<TabTitleText>Overview</TabTitleText>}
            {...overviewTab}
          >
            <OverviewTab />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
