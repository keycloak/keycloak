import {
  Avatar,
  Divider,
  Dropdown,
  DropdownItem,
  DropdownList,
  Masthead,
  MastheadBrand,
  MastheadContent,
  MastheadToggle,
  MenuToggle,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { BarsIcon, EllipsisVIcon, HelpIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useHref } from "react-router-dom";
import { useEnvironment, useHelp } from "@keycloak/keycloak-ui-shared";
import { HelpHeader } from "./components/help-enabler/HelpHeader";
import { useRealm } from "./context/realm-context/RealmContext";
import { useWhoAmI } from "./context/whoami/WhoAmI";
import { toDashboard } from "./dashboard/routes/Dashboard";

const ManageAccountDropdownItem = () => {
  const { keycloak } = useEnvironment();

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
  const { keycloak } = useEnvironment();
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
      component={(props) => <Link {...props} to={toDashboard({ realm })} />}
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
  <Divider component="li" key="kebab sign out separator" />,
  <SignOutDropdownItem key="kebab Sign out" />,
];

const userDropdownItems = [
  <ManageAccountDropdownItem key="Manage Account" />,
  <ServerInfoDropdownItem key="Server info" />,
  <Divider component="li" key="sign out separator" />,
  <SignOutDropdownItem key="Sign out" />,
];

const KebabDropdown = () => {
  const [isDropdownOpen, setDropdownOpen] = useState(false);

  return (
    <Dropdown
      isPlain
      onOpenChange={(isOpen) => setDropdownOpen(isOpen)}
      toggle={(ref) => (
        <MenuToggle
          id="user-dropdown-kebab"
          ref={ref}
          variant="plain"
          onClick={() => setDropdownOpen(!isDropdownOpen)}
          isExpanded={isDropdownOpen}
        >
          <EllipsisVIcon />
        </MenuToggle>
      )}
      isOpen={isDropdownOpen}
    >
      <DropdownList>{kebabDropdownItems}</DropdownList>
    </Dropdown>
  );
};

const UserDropdown = () => {
  const { whoAmI } = useWhoAmI();
  const [isDropdownOpen, setDropdownOpen] = useState(false);

  return (
    <Dropdown
      isPlain
      isOpen={isDropdownOpen}
      onOpenChange={(isOpen) => setDropdownOpen(isOpen)}
      toggle={(ref) => (
        <MenuToggle
          id="user-dropdown"
          ref={ref}
          onClick={() => setDropdownOpen(!isDropdownOpen)}
        >
          {whoAmI.getDisplayName()}
        </MenuToggle>
      )}
    >
      <DropdownList>{userDropdownItems}</DropdownList>
    </Dropdown>
  );
};

export const Header = () => {
  const { environment, keycloak } = useEnvironment();
  const { t } = useTranslation();
  const { realm } = useRealm();

  const picture = keycloak.tokenParsed?.picture;
  const logo = environment.logo ? environment.logo : "/logo.svg";
  const url = useHref(toDashboard({ realm }));
  const logoUrl = environment.logoUrl ? environment.logoUrl : url;

  return (
    <Masthead>
      <MastheadToggle>
        <PageToggleButton variant="plain" aria-label={t("navigation")}>
          <BarsIcon />
        </PageToggleButton>
      </MastheadToggle>
      <MastheadBrand href={logoUrl}>
        <img
          src={environment.resourceUrl + logo}
          id="masthead-logo"
          alt={t("logo")}
          aria-label={t("logo")}
          className="keycloak__pageheader_brand"
        />
      </MastheadBrand>
      <MastheadContent>
        <Toolbar>
          <ToolbarContent>
            <ToolbarItem
              align={{ default: "alignRight" }}
              visibility={{
                default: "hidden",
                md: "visible",
              }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
            >
              <HelpHeader />
            </ToolbarItem>
            <ToolbarItem
              align={{ default: "alignLeft" }}
              visibility={{
                md: "hidden",
              }} /** this kebab dropdown replaces the icon buttons and is hidden for desktop sizes */
            >
              <KebabDropdown />
            </ToolbarItem>
            <ToolbarItem
              visibility={{
                default: "hidden",
                md: "visible",
              }} /** this user dropdown is hidden on mobile sizes */
            >
              <UserDropdown />
            </ToolbarItem>
            <ToolbarItem
              variant="overflow-menu"
              align={{ default: "alignRight" }}
              className="pf-v5-u-m-0-on-lg"
            >
              <Avatar
                src={picture || environment.resourceUrl + "/img_avatar.svg"}
                alt={t("avatarImage")}
                aria-label={t("avatarImage")}
              />
            </ToolbarItem>
          </ToolbarContent>
        </Toolbar>
      </MastheadContent>
    </Masthead>
  );
};
