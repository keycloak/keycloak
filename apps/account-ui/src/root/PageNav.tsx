import {
  Nav,
  NavExpandable,
  NavItem,
  NavList,
  PageSidebar,
} from "@patternfly/react-core";
import { TFuncKey } from "i18next";
import {
  FunctionComponent,
  MouseEvent as ReactMouseEvent,
  useMemo,
} from "react";
import { useTranslation } from "react-i18next";
import {
  matchPath,
  To,
  useHref,
  useLinkClickHandler,
  useLocation,
} from "react-router-dom";

type RootMenuItem = {
  label: TFuncKey;
  path: string;
};

type MenuItemWithChildren = {
  label: TFuncKey;
  children: MenuItem[];
};

type MenuItem = RootMenuItem | MenuItemWithChildren;

const menuItems: MenuItem[] = [
  {
    label: "personalInfo",
    path: "personal-info",
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
  },
  {
    label: "resources",
    path: "resources",
  },
];

export const PageNav = () => (
  <PageSidebar
    nav={
      <Nav>
        <NavList>
          {menuItems.map((menuItem) => (
            <NavMenuItem key={menuItem.label} menuItem={menuItem} />
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
    [pathname, menuItem]
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
      {menuItem.children.map((child) => (
        <NavMenuItem key={child.label} menuItem={child} />
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

const NavLink: FunctionComponent<NavLinkProps> = ({
  to,
  isActive,
  children,
}) => {
  const href = useHref(to);
  const handleClick = useLinkClickHandler(to);

  return (
    <NavItem
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
