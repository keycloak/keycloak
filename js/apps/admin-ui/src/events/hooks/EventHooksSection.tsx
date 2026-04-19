import { Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import { EventHookLogs } from "./EventHookLogs";
import { EventHookTargets } from "./EventHookTargets";
import { toEvents } from "../routes/Events";

export const EventHooksSection = () => {
  const { realm } = useRealm();
  const { t } = useTranslation();
  const targetsTab = useRoutableTab(
    toEvents({ realm, tab: "hooks", subTab: "targets" }),
  );
  const logsTab = useRoutableTab(
    toEvents({ realm, tab: "hooks", subTab: "logs" }),
  );

  return (
    <RoutableTabs
      defaultLocation={toEvents({ realm, tab: "hooks", subTab: "targets" })}
      mountOnEnter
    >
      <Tab title={<TabTitleText>{t("targets")}</TabTitleText>} {...targetsTab}>
        <EventHookTargets />
      </Tab>
      <Tab title={<TabTitleText>{t("logs")}</TabTitleText>} {...logsTab}>
        <EventHookLogs />
      </Tab>
    </RoutableTabs>
  );
};
