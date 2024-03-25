import { Avatar, Brand } from "@patternfly/react-core";
import {
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  DropdownToggle,
  KebabToggle,
  PageHeader,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem,
} from "@patternfly/react-core/deprecated";
import { HelpIcon } from "@patternfly/react-icons";
import { ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useHelp } from "ui-shared";

import { HelpHeader } from "./components/help-enabler/HelpHeader";
import { useRealm } from "./context/realm-context/RealmContext";
import { useWhoAmI } from "./context/whoami/WhoAmI";
import { toDashboard } from "./dashboard/routes/Dashboard";
import environment from "./environment";
import { keycloak } from "./keycloak";

const ManageAccountDropdownItem = () => {
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
    <DropdownItem
      data-testId="helpIcon"
      icon={<HelpIcon />}
      onClick={toggleHelp}
    >
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
      toggle={<KebabToggle onToggle={(_event, val) => setDropdownOpen(val)} />}
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
        <DropdownToggle onToggle={(_event, val) => setDropdownOpen(val)}>
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
    const picture = keycloak.tokenParsed?.picture;
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

  const logo = environment.logo ? environment.logo : "/logo.svg";
  const logoUrl = environment.logoUrl
    ? environment.logoUrl
    : toDashboard({ realm });

  return (
    <PageHeader
      showNavToggle
      logo={
        <Link to={logoUrl}>
          <Brand
            src={environment.resourceUrl + logo}
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
