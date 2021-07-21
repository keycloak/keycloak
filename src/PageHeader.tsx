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
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";
import { HelpHeader, useHelp } from "./components/help-enabler/HelpHeader";
import { useAdminClient } from "./context/auth/AdminClient";
import { useWhoAmI } from "./context/whoami/WhoAmI";
import environment from "./environment";

export const Header = () => {
  const adminClient = useAdminClient();
  const { t } = useTranslation();

  const ManageAccountDropdownItem = () => {
    return (
      <>
        {adminClient.keycloak && (
          <DropdownItem
            key="manage account"
            id="manage-account"
            onClick={() => adminClient.keycloak.accountManagement()}
          >
            {t("manageAccount")}
          </DropdownItem>
        )}
      </>
    );
  };

  const SignOutDropdownItem = () => {
    return (
      <>
        {adminClient.keycloak && (
          <DropdownItem
            id="sign-out"
            key="sign out"
            onClick={() => adminClient.keycloak.logout({ redirectUri: "" })}
          >
            {t("signOut")}
          </DropdownItem>
        )}
      </>
    );
  };

  const ServerInfoDropdownItem = () => {
    const { t } = useTranslation();
    const history = useHistory();
    return (
      <DropdownItem
        key="server info"
        onClick={() => {
          history.push("/master/");
        }}
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
        <Avatar
          src={environment.resourceUrl + "/img_avatar.svg"}
          alt="Avatar image"
        />
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
        id="user-dropdown-kebab"
        isPlain
        position="right"
        toggle={<KebabToggle onToggle={onDropdownToggle} />}
        isOpen={isDropdownOpen}
        dropdownItems={kebabDropdownItems}
      />
    );
  };

  const UserDropdown = () => {
    const { whoAmI } = useWhoAmI();
    const [isDropdownOpen, setDropdownOpen] = useState(false);

    const onDropdownToggle = () => {
      setDropdownOpen(!isDropdownOpen);
    };

    return (
      <Dropdown
        isPlain
        position="right"
        id="user-dropdown"
        isOpen={isDropdownOpen}
        toggle={
          <DropdownToggle onToggle={onDropdownToggle}>
            {whoAmI.getDisplayName()}
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
