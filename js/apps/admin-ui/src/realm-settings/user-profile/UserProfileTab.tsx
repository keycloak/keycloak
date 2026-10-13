import { Tab, TabTitleText } from "@patternfly/react-core";

import { useTranslation } from "react-i18next";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { useRealm } from "../../context/realm-context/RealmContext";
import {
  toUserProfile,
  UserProfileTab as IUserProfileTab,
} from "../routes/UserProfile";
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

  const useTab = (tab: IUserProfileTab) =>
    useRoutableTab(toUserProfile({ realm, tab }));

  const attributesTab = useTab("attributes");
  const attributesGroupTab = useTab("attributes-group");
  const jsonEditorTab = useTab("json-editor");

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
