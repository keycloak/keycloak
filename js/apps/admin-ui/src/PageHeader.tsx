import {
  KeycloakMasthead,
  useEnvironment,
  useHelp,
} from "@keycloak/keycloak-ui-shared";
import { DropdownItem, ToolbarItem } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { Link, useHref } from "react-router-dom";
import { PageHeaderClearCachesModal } from "./PageHeaderClearCachesModal";
import { HelpHeader } from "./components/help-enabler/HelpHeader";
import { useAccess } from "./context/access/Access";
import { useRealm } from "./context/realm-context/RealmContext";
import { toDashboard } from "./dashboard/routes/Dashboard";
import { usePreviewLogo } from "./realm-settings/themes/LogoContext";
import { joinPath } from "./utils/joinPath";
import useToggle from "./utils/useToggle";

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

const ClearCachesDropdownItem = () => {
  const { t } = useTranslation();
  const [open, toggleModal] = useToggle();

  return (
    <>
      <DropdownItem key="clear caches" onClick={() => toggleModal()}>
        {t("clearCachesTitle")}
      </DropdownItem>
      {open && <PageHeaderClearCachesModal onClose={() => toggleModal()} />}
    </>
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

const kebabDropdownItems = (isMasterRealm: boolean, isManager: boolean) => [
  <ManageAccountDropdownItem key="kebab Manage Account" />,
  <ServerInfoDropdownItem key="kebab Server Info" />,
  ...(isMasterRealm && isManager
    ? [<ClearCachesDropdownItem key="Clear Caches" />]
    : []),
  <HelpDropdownItem key="kebab Help" />,
];

const userDropdownItems = (isMasterRealm: boolean, isManager: boolean) => [
  <ManageAccountDropdownItem key="Manage Account" />,
  <ServerInfoDropdownItem key="Server info" />,
  ...(isMasterRealm && isManager
    ? [<ClearCachesDropdownItem key="Clear Caches" />]
    : []),
];

export const Header = () => {
  const { environment, keycloak } = useEnvironment();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { hasAccess } = useAccess();

  const contextLogo = usePreviewLogo();
  const customLogo = contextLogo?.logo;

  const isMasterRealm = realm === "master";
  const isManager = hasAccess("manage-realm");

  const logo = customLogo || environment.logo || "/logo.svg";
  const url = useHref(toDashboard({ realm }));
  const logoUrl = environment.logoUrl ? environment.logoUrl : url;

  return (
    <KeycloakMasthead
      data-testid="page-header"
      keycloak={keycloak}
      features={{ hasManageAccount: false }}
      brand={{
        href: logoUrl,
        src: logo.startsWith("/")
          ? joinPath(environment.resourceUrl, logo)
          : logo,
        alt: t("logo"),
        className: "keycloak__pageheader_brand",
      }}
      dropdownItems={userDropdownItems(isMasterRealm, isManager)}
      kebabDropdownItems={kebabDropdownItems(isMasterRealm, isManager)}
      toolbarItems={[
        <ToolbarItem
          key="help"
          align={{ default: "alignRight" }}
          visibility={{
            default: "hidden",
            md: "visible",
          }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
        >
          <HelpHeader />
        </ToolbarItem>,
      ]}
    />
  );
};
