import { Tab, TabTitleText } from "@patternfly/react-core";

import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toUserProfile } from "../routes/UserProfile";
import { AttributesGroupTab } from "./AttributesGroupTab";
import { AttributesTab } from "./AttributesTab";
import { JsonEditorTab } from "./JsonEditorTab";
import { UserProfileProvider } from "./UserProfileContext";

type UserProfileTabProps = {
  setTableData: React.Dispatch<
    React.SetStateAction<Record<string, string>[] | undefined>
  >;
};

export const UserProfileTab = ({ setTableData }: UserProfileTabProps) => {
  const { realm } = useRealm();
  const { t } = useTranslation();

  const attributesTab = useRoutableTab(
    toUserProfile({ realm, tab: "attributes" }),
  );
  const attributesGroupTab = useRoutableTab(
    toUserProfile({ realm, tab: "attributes-group" }),
  );
  const jsonEditorTab = useRoutableTab(
    toUserProfile({ realm, tab: "json-editor" }),
  );

  return (
    <UserProfileProvider>
      <RoutableTabs
        defaultLocation={toUserProfile({ realm, tab: "attributes" })}
        mountOnEnter
      >
        <Tab
          title={<TabTitleText>{t("attributes")}</TabTitleText>}
          data-testid="attributesTab"
          {...attributesTab}
        >
          <AttributesTab setTableData={setTableData} />
        </Tab>
        <Tab
          title={<TabTitleText>{t("attributesGroup")}</TabTitleText>}
          data-testid="attributesGroupTab"
          {...attributesGroupTab}
        >
          <AttributesGroupTab setTableData={setTableData} />
        </Tab>
        <Tab
          title={<TabTitleText>{t("jsonEditor")}</TabTitleText>}
          data-testid="jsonEditorTab"
          {...jsonEditorTab}
        >
          <JsonEditorTab />
        </Tab>
      </RoutableTabs>
    </UserProfileProvider>
  );
};
