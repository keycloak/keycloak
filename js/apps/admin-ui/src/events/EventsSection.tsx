import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import helpUrls from "../help-urls";
import { toRealmSettings } from "../realm-settings/routes/RealmSettings";
import { AdminEvents } from "./AdminEvents";
import { UserEvents } from "./UserEvents";
import { EventsTab, toEvents } from "./routes/Events";

import "./events.css";

export default function EventsSection() {
  const { t } = useTranslation();
  const { realm } = useRealm();

  const useTab = (tab: EventsTab) => useRoutableTab(toEvents({ realm, tab }));

  const userEventsTab = useTab("user-events");
  const adminEventsTab = useTab("admin-events");

  return (
    <>
      <ViewHeader
        titleKey="titleEvents"
        subKey={
          <Trans i18nKey="eventExplain">
            If you want to configure user events, Admin events or Event
            listeners, please enter
            <Link to={toRealmSettings({ realm, tab: "events" })}>
              {t("eventConfig")}
            </Link>
            page realm settings to configure.
          </Trans>
        }
        helpUrl={helpUrls.eventsUrl}
        divider={false}
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <RoutableTabs
          isBox
          defaultLocation={toEvents({ realm, tab: "user-events" })}
        >
          <Tab
            title={<TabTitleText>{t("userEvents")}</TabTitleText>}
            {...userEventsTab}
          >
            <UserEvents />
          </Tab>
          <Tab
            title={<TabTitleText>{t("adminEvents")}</TabTitleText>}
            data-testid="admin-events-tab"
            {...adminEventsTab}
          >
            <AdminEvents />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
