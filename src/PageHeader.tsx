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
import { WhoAmIContext } from "./context/whoami/WhoAmI";
import { HelpHeader } from "./components/help-enabler/HelpHeader";
import { Link } from "react-router-dom";
import { useAdminClient } from "./context/auth/AdminClient";

export const Header = () => {
  const adminClient = useAdminClient();
  const { t } = useTranslation();

  const ManageAccountDropdownItem = () => {
    return (
      <DropdownItem
        key="manage account"
        onClick={() => adminClient.keycloak.accountManagement()}
      >
        {t("manageAccount")}
      </DropdownItem>
    );
  };

  const SignOutDropdownItem = () => {
    return (
      <DropdownItem
        key="sign out"
        onClick={() => adminClient.keycloak.logout({ redirectUri: "" })}
      >
        {t("signOut")}
      </DropdownItem>
    );
  };

  const ServerInfoDropdownItem = () => {
    const { t } = useTranslation();
    return <DropdownItem key="server info">{t("serverInfo")}</DropdownItem>;
  };

  const HelpDropdownItem = () => {
    const { t } = useTranslation();
    return <DropdownItem icon={<HelpIcon />}>{t("help")}</DropdownItem>;
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
    const whoami = useContext(WhoAmIContext);
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
            {whoami.getDisplayName()}
          </DropdownToggle>
        }
        dropdownItems={userDropdownItems}
      />
    );
  };

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
