import {
  Nav,
  NavExpandable,
  NavItem,
  NavList,
  PageSidebar,
} from "@patternfly/react-core";
import {
  PropsWithChildren,
  MouseEvent as ReactMouseEvent,
  useMemo,
} from "react";
import { useTranslation } from "react-i18next";
import {
  To,
  matchPath,
  useHref,
  useLinkClickHandler,
  useLocation,
} from "react-router-dom";
import { environment } from "../environment";
import { TFuncKey } from "../i18n";

type RootMenuItem = {
  label: TFuncKey;
  path: string;
  isHidden?: boolean;
};

type MenuItemWithChildren = {
  label: TFuncKey;
  children: MenuItem[];
  isHidden?: boolean;
};

type MenuItem = RootMenuItem | MenuItemWithChildren;

const menuItems: MenuItem[] = [
  {
    label: "personalInfo",
    path: "/",
  },
  {
    label: "accountSecurity",
    children: [
      {
        label: "signingIn",
        path: "account-security/signing-in",
      },
      {
        label: "deviceActivity",
        path: "account-security/device-activity",
      },
      {
        label: "linkedAccounts",
        path: "account-security/linked-accounts",
        isHidden: !environment.features.isLinkedAccountsEnabled,
      },
    ],
  },
  {
    label: "applications",
    path: "applications",
  },
  {
    label: "groups",
    path: "groups",
    isHidden: !environment.features.isViewGroupsEnabled,
  },
  {
    label: "resources",
    path: "resources",
    isHidden: !environment.features.isMyResourcesEnabled,
  },
];

export const PageNav = () => (
  <PageSidebar
    nav={
      <Nav>
        <NavList>
          {menuItems
            .filter((menuItem) => !menuItem.isHidden)
            .map((menuItem) => (
              <NavMenuItem key={menuItem.label as string} menuItem={menuItem} />
            ))}
        </NavList>
      </Nav>
    }
  />
);

type NavMenuItemProps = {
  menuItem: MenuItem;
};

function NavMenuItem({ menuItem }: NavMenuItemProps) {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const isActive = useMemo(
    () => matchMenuItem(pathname, menuItem),
    [pathname, menuItem],
  );

  if ("path" in menuItem) {
    return (
      <NavLink to={menuItem.path} isActive={isActive}>
        {t(menuItem.label)}
      </NavLink>
    );
  }

  return (
    <NavExpandable
      title={t(menuItem.label)}
      isActive={isActive}
      isExpanded={isActive}
    >
      {menuItem.children
        .filter((menuItem) => !menuItem.isHidden)
        .map((child) => (
          <NavMenuItem key={child.label as string} menuItem={child} />
        ))}
    </NavExpandable>
  );
}

function matchMenuItem(currentPath: string, menuItem: MenuItem): boolean {
  if ("path" in menuItem) {
    return !!matchPath(menuItem.path, currentPath);
  }

  return menuItem.children.some((child) => matchMenuItem(currentPath, child));
}

type NavLinkProps = {
  to: To;
  isActive: boolean;
};

const NavLink = ({
  to,
  isActive,
  children,
}: PropsWithChildren<NavLinkProps>) => {
  const href = useHref(to);
  const handleClick = useLinkClickHandler(to);

  return (
    <NavItem
      data-testid={to}
      to={href}
      isActive={isActive}
      onClick={(event) =>
        // PatternFly does not have the correct type for this event, so we need to cast it.
        handleClick(event as unknown as ReactMouseEvent<HTMLAnchorElement>)
      }
    >
      {children}
    </NavItem>
  );
};
