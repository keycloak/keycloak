import { useTranslation } from "react-i18next";
import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";

import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import helpUrls from "../help-urls";
import { PermissionsTab } from "../components/permission-tab/PermissionTab";
import { UserDataTable } from "../components/users/UserDataTable";
import { toUsers, UserTab } from "./routes/Users";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import "./user-section.css";
import { useAccess } from "../context/access/Access";

export default function UsersSection() {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const { hasAccess } = useAccess();
  const isFeatureEnabled = useIsFeatureEnabled();

  const canViewPermissions =
    isFeatureEnabled(Feature.AdminFineGrainedAuthz) &&
    hasAccess("manage-authorization", "manage-users", "manage-clients");

  const useTab = (tab: UserTab) =>
    useRoutableTab(
      toUsers({
        realm: realmName,
        tab,
      }),
    );

  const listTab = useTab("list");
  const permissionsTab = useTab("permissions");

  return (
    <>
      <ViewHeader
        titleKey="titleUsers"
        subKey="usersExplain"
        helpUrl={helpUrls.usersUrl}
        divider={false}
      />
      <PageSection
        data-testid="users-page"
        variant="light"
        className="pf-v5-u-p-0"
      >
        <RoutableTabs
          data-testid="user-tabs"
          defaultLocation={toUsers({
            realm: realmName,
            tab: "list",
          })}
          isBox
          mountOnEnter
        >
          <Tab
            id="list"
            data-testid="listTab"
            title={<TabTitleText>{t("userList")}</TabTitleText>}
            {...listTab}
          >
            <UserDataTable />
          </Tab>
          {canViewPermissions && (
            <Tab
              id="permissions"
              data-testid="permissionsTab"
              title={<TabTitleText>{t("permissions")}</TabTitleText>}
              {...permissionsTab}
            >
              <PermissionsTab type="users" />
            </Tab>
          )}
        </RoutableTabs>
      </PageSection>
    </>
  );
}
