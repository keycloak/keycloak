import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
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
  PageHeaderToolsItem,
  PageHeaderToolsGroup,
} from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { KeycloakContext } from "./auth/KeycloakContext";
import { HelpHeader } from "./components/help-enabler/HelpHeader";
import { Link } from "react-router-dom";

export const Header = () => {
  return (
    <PageHeader
      showNavToggle
      logo={
        <Link to="/">
          <Brand src="/logo.svg" alt="Logo" />
        </Link>
      }
      logoComponent="div"
      headerTools={headerTools()}
    />
  );
};

const ManageAccountDropdownItem = () => {
  const keycloak = useContext(KeycloakContext);
  const { t } = useTranslation();
  return (
    <DropdownItem key="manage account" onClick={() => keycloak?.account()}>
      {t("Manage account")}
    </DropdownItem>
  );
};

const SignOutDropdownItem = () => {
  const keycloak = useContext(KeycloakContext);
  const { t } = useTranslation();
  return (
    <DropdownItem key="sign out" onClick={() => keycloak?.logout()}>
      {t("Sign out")}
    </DropdownItem>
  );
};

const ServerInfoDropdownItem = () => {
  const { t } = useTranslation();
  return <DropdownItem key="server info">{t("Server info")}</DropdownItem>;
};

const HelpDropdownItem = () => {
  const { t } = useTranslation();
  const help = t("Help");
  return <DropdownItem icon={<HelpIcon />}>{`${help}`}</DropdownItem>;
};

const kebabDropdownItems = [
  <ManageAccountDropdownItem key="kebab Manage Account" />,
  <ServerInfoDropdownItem key="kebab Server Info" />,
  <HelpDropdownItem key="kebab Help" />,
  <DropdownSeparator key="kebab sign out seperator" />,
  <SignOutDropdownItem key="kebab Sign out" />,
];

const userDropdownItems = [
  <ManageAccountDropdownItem key="Manage Account" />,
  <ServerInfoDropdownItem key="Server info" />,
  <DropdownSeparator key="sign out seperator" />,
  <SignOutDropdownItem key="Sign out" />,
];

const headerTools = () => {
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
      <Avatar src="/img_avatar.svg" alt="Avatar image" />
    </PageHeaderTools>
  );
};

const KebabDropdown = () => {
  const [isDropdownOpen, setDropdownOpen] = useState(false);

  const onDropdownToggle = () => {
    setDropdownOpen(!isDropdownOpen);
  };

  return (
    <Dropdown
      isPlain
      position="right"
      toggle={<KebabToggle onToggle={onDropdownToggle} />}
      isOpen={isDropdownOpen}
      dropdownItems={kebabDropdownItems}
    />
  );
};

const UserDropdown = () => {
  const keycloak = useContext(KeycloakContext);
  const [isDropdownOpen, setDropdownOpen] = useState(false);

  const onDropdownToggle = () => {
    setDropdownOpen(!isDropdownOpen);
  };

  return (
    <Dropdown
      isPlain
      position="right"
      isOpen={isDropdownOpen}
      toggle={
        <DropdownToggle onToggle={onDropdownToggle}>
          {keycloak?.loggedInUser}
        </DropdownToggle>
      }
      dropdownItems={userDropdownItems}
    />
  );
};
