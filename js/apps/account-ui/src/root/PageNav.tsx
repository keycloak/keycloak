import {
  Nav,
  NavExpandable,
  NavItem,
  NavList,
  PageSidebar,
  Spinner,
  PageSidebarBody,
} from "@patternfly/react-core";
import {
  PropsWithChildren,
  MouseEvent as ReactMouseEvent,
  Suspense,
  useMemo,
  useState,
} from "react";
import { useTranslation } from "react-i18next";
import {
  To,
  matchPath,
  useHref,
  useLinkClickHandler,
  useLocation,
} from "react-router-dom";
import fetchContentJson from "../content/fetchContent";
import { environment, type Feature } from "../environment";
import { TFuncKey } from "../i18n";
import { usePromise } from "../utils/usePromise";
import { useEnvironment } from "./KeycloakContext";

type RootMenuItem = {
  label: TFuncKey;
  path: string;
  isVisible?: keyof Feature;
  modulePath?: string;
};

type MenuItemWithChildren = {
  label: TFuncKey;
  children: MenuItem[];
  isVisible?: keyof Feature;
};

export type MenuItem = RootMenuItem | MenuItemWithChildren;

export const PageNav = () => {
  const [menuItems, setMenuItems] = useState<MenuItem[]>();
  const context = useEnvironment();

  usePromise((signal) => fetchContentJson({ signal, context }), setMenuItems);
  return (
    <PageSidebar>
      <PageSidebarBody>
        <Nav>
          <NavList>
            <Suspense fallback={<Spinner />}>
              {menuItems
                ?.filter((menuItem) =>
                  menuItem.isVisible
                    ? context.environment.features[menuItem.isVisible]
                    : true,
                )
                .map((menuItem) => (
                  <NavMenuItem
                    key={menuItem.label as string}
                    menuItem={menuItem}
                  />
                ))}
            </Suspense>
          </NavList>
        </Nav>
      </PageSidebarBody>
    </PageSidebar>
  );
};

type NavMenuItemProps = {
  menuItem: MenuItem;
};

function NavMenuItem({ menuItem }: NavMenuItemProps) {
  const { t } = useTranslation();
  const {
    environment: { features },
  } = useEnvironment();
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
      data-testid={menuItem.label}
      title={t(menuItem.label)}
      isActive={isActive}
      isExpanded={isActive}
    >
      {menuItem.children
        .filter((menuItem) =>
          menuItem.isVisible ? features[menuItem.isVisible] : true,
        )
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

export const NavLink = ({
  to,
  isActive,
  children,
}: PropsWithChildren<NavLinkProps>) => {
  const menuItemPath = `${new URL(environment.baseUrl).pathname}${to}`;
  const href = useHref(menuItemPath);
  const handleClick = useLinkClickHandler(menuItemPath);

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
