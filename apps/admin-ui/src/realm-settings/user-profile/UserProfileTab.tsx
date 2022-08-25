import { Tab, TabTitleText } from "@patternfly/react-core";

import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import {
  routableTab,
  RoutableTabs,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toUserProfile } from "../routes/UserProfile";
import { AttributesGroupTab } from "./AttributesGroupTab";
import { AttributesTab } from "./AttributesTab";
import { JsonEditorTab } from "./JsonEditorTab";
import { UserProfileProvider } from "./UserProfileContext";

export const UserProfileTab = () => {
  const { realm } = useRealm();
  const { t } = useTranslation("realm-settings");
  const history = useHistory();

  return (
    <UserProfileProvider>
      <RoutableTabs
        defaultLocation={toUserProfile({ realm, tab: "attributes" })}
        mountOnEnter
      >
        <Tab
          title={<TabTitleText>{t("attributes")}</TabTitleText>}
          data-testid="attributesTab"
          {...routableTab({
            to: toUserProfile({ realm, tab: "attributes" }),
            history,
          })}
        >
          <AttributesTab />
        </Tab>
        <Tab
          title={<TabTitleText>{t("attributesGroup")}</TabTitleText>}
          data-testid="attributesGroupTab"
          {...routableTab({
            to: toUserProfile({ realm, tab: "attributes-group" }),
            history,
          })}
        >
          <AttributesGroupTab />
        </Tab>
        <Tab
          title={<TabTitleText>{t("jsonEditor")}</TabTitleText>}
          data-testid="jsonEditorTab"
          {...routableTab({
            to: toUserProfile({ realm, tab: "json-editor" }),
            history,
          })}
        >
          <JsonEditorTab />
        </Tab>
      </RoutableTabs>
    </UserProfileProvider>
  );
};
