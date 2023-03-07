import {
  Avatar,
  Brand,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  DropdownToggle,
  KebabToggle,
  PageHeader,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem,
} from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { HelpHeader } from "./components/help-enabler/HelpHeader";
import { useHelp } from "ui-shared";
import { useAdminClient } from "./context/auth/AdminClient";
import { useRealm } from "./context/realm-context/RealmContext";
import { useWhoAmI } from "./context/whoami/WhoAmI";
import { toDashboard } from "./dashboard/routes/Dashboard";
import environment from "./environment";

const ManageAccountDropdownItem = () => {
  const { keycloak } = useAdminClient();
  const { t } = useTranslation();
  return (
    <DropdownItem
      key="manage account"
      id="manage-account"
      onClick={() => keycloak.accountManagement()}
    >
      {t("manageAccount")}
    </DropdownItem>
  );
};

const SignOutDropdownItem = () => {
  const { keycloak } = useAdminClient();
  const { t } = useTranslation();
  return (
    <DropdownItem
      id="sign-out"
      key="sign out"
      onClick={() => keycloak.logout({ redirectUri: "" })}
    >
      {t("signOut")}
    </DropdownItem>
  );
};

const ServerInfoDropdownItem = () => {
  const { realm } = useRealm();
  const { t } = useTranslation();

  return (
    <DropdownItem
      key="server info"
      component={
        // The type definition in PatternFly is incorrect, so we need to cast here.
        ((props: any) => (
          <Link {...props} to={toDashboard({ realm })} />
        )) as unknown as ReactNode
      }
    >
      {t("realmInfo")}
    </DropdownItem>
  );
};

const HelpDropdownItem = () => {
  const { t } = useTranslation();
  const { enabled, toggleHelp } = useHelp();
  return (
    <DropdownItem icon={<HelpIcon />} onClick={toggleHelp}>
      {enabled ? t("helpEnabled") : t("helpDisabled")}
    </DropdownItem>
  );
};

const kebabDropdownItems = [
  <ManageAccountDropdownItem key="kebab Manage Account" />,
  <ServerInfoDropdownItem key="kebab Server Info" />,
  <HelpDropdownItem key="kebab Help" />,
  <DropdownSeparator key="kebab sign out separator" />,
  <SignOutDropdownItem key="kebab Sign out" />,
];

const userDropdownItems = [
  <ManageAccountDropdownItem key="Manage Account" />,
  <ServerInfoDropdownItem key="Server info" />,
  <DropdownSeparator key="sign out separator" />,
  <SignOutDropdownItem key="Sign out" />,
];

const KebabDropdown = () => {
  const [isDropdownOpen, setDropdownOpen] = useState(false);

  return (
    <Dropdown
      id="user-dropdown-kebab"
      isPlain
      position="right"
      toggle={<KebabToggle onToggle={setDropdownOpen} />}
      isOpen={isDropdownOpen}
      dropdownItems={kebabDropdownItems}
    />
  );
};

const UserDropdown = () => {
  const { whoAmI } = useWhoAmI();
  const [isDropdownOpen, setDropdownOpen] = useState(false);

  return (
    <Dropdown
      isPlain
      position="right"
      id="user-dropdown"
      isOpen={isDropdownOpen}
      toggle={
        <DropdownToggle onToggle={setDropdownOpen}>
          {whoAmI.getDisplayName()}
        </DropdownToggle>
      }
      dropdownItems={userDropdownItems}
    />
  );
};

export const Header = () => {
  const { realm } = useRealm();

  const headerTools = () => {
    const adminClient = useAdminClient();
    const picture = adminClient.keycloak.tokenParsed?.picture;
    return (
      <PageHeaderTools>
        <PageHeaderToolsGroup
          visibility={{
            default: "hidden",
            md: "visible",
          }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
        >
          <PageHeaderToolsItem>
            <HelpHeader />
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>

        <PageHeaderToolsGroup>
          <PageHeaderToolsItem
            visibility={{
              md: "hidden",
            }} /** this kebab dropdown replaces the icon buttons and is hidden for desktop sizes */
          >
            <KebabDropdown />
          </PageHeaderToolsItem>
          <PageHeaderToolsItem
            visibility={{
              default: "hidden",
              md: "visible",
            }} /** this user dropdown is hidden on mobile sizes */
          >
            <UserDropdown />
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>
        <Avatar
          src={picture || environment.resourceUrl + "/img_avatar.svg"}
          alt="Avatar image"
        />
      </PageHeaderTools>
    );
  };

  return (
    <PageHeader
      showNavToggle
      logo={
        <Link to={toDashboard({ realm })}>
          <Brand
            src={environment.resourceUrl + "/logo.svg"}
            id="masthead-logo"
            alt="Logo"
            className="keycloak__pageheader_brand"
          />
        </Link>
      }
      logoComponent="div"
      headerTools={headerTools()}
    />
  );
};
