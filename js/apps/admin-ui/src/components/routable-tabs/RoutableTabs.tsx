import {
  Tab,
  TabProps,
  Tabs,
  TabsComponent,
  TabsProps,
} from "@patternfly/react-core";
import {
  Children,
  JSXElementConstructor,
  PropsWithChildren,
  ReactElement,
  isValidElement,
} from "react";
import {
  Path,
  generatePath,
  matchPath,
  useHref,
  useLocation,
  useParams,
} from "react-router-dom";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { PageHandler } from "../../page/PageHandler";
import { TAB_PROVIDER } from "../../page/constants";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { useTranslation } from "react-i18next";

// TODO: Remove the custom 'children' props and type once the following issue has been resolved:
// https://github.com/patternfly/patternfly-react/issues/6766
type ChildElement = ReactElement<TabProps, JSXElementConstructor<TabProps>>;
type Child = ChildElement | boolean | null | undefined;

// TODO: Figure out why we need to omit 'ref' from the props.
type RoutableTabsProps = {
  children: Child | Child[];
  defaultLocation?: Partial<Path>;
} & Omit<
  TabsProps,
  "ref" | "activeKey" | "defaultActiveKey" | "component" | "children"
>;

export const RoutableTabs = ({
  children,
  defaultLocation,
  ...otherProps
}: RoutableTabsProps) => {
  const { pathname } = useLocation();
  const params = useParams();
  const { componentTypes } = useServerInfo();
  const tabs = componentTypes?.[TAB_PROVIDER] || [];
  const isFeatureEnabled = useIsFeatureEnabled();
  const { t } = useTranslation();

  const matchedTabs = tabs
    .filter((tab) => matchPath({ path: tab.metadata.path }, pathname))
    .map((t) => ({
      ...t,
      pathname: generatePath(t.metadata.path, {
        ...params,
        ...t.metadata.params,
      }),
    }));
  // Extract all keys from matchedTabs
  const matchedTabsKeys = matchedTabs.map((t) => t.pathname);

  // Extract event keys from children
  const eventKeys = Children.toArray(children)
    .filter((child): child is ChildElement => isValidElement(child))
    .map((child) => child.props.eventKey.toString());

  const allKeys = [...eventKeys, ...matchedTabsKeys];

  // Determine if there is an exact match.
  const exactMatch = allKeys.find(
    (eventKey) => eventKey === decodeURI(pathname),
  );

  // Determine which event keys at least partially match the current path, then sort them so the nearest match ends up on top.
  const nearestMatch = eventKeys
    .filter((eventKey) => pathname.includes(eventKey))
    .sort((a, b) => a.length - b.length)
    .pop();

  return (
    <Tabs
      activeKey={
        exactMatch ?? nearestMatch ?? defaultLocation?.pathname ?? pathname
      }
      component={TabsComponent.nav}
      inset={{
        default: "insetNone",
        xl: "insetLg",
        "2xl": "inset2xl",
      }}
      unmountOnExit
      mountOnEnter
      {...otherProps}
    >
      {children as any}
      {isFeatureEnabled(Feature.DeclarativeUI) &&
        matchedTabs.map<any>((tab) => (
          <DynamicTab key={tab.id} eventKey={tab.pathname} title={t(tab.id)}>
            <PageHandler page={tab} providerType={TAB_PROVIDER} />
          </DynamicTab>
        ))}
    </Tabs>
  );
};

type DynamicTabProps = {
  title: string;
  eventKey: string;
};

const DynamicTab = ({
  children,
  ...props
}: PropsWithChildren<DynamicTabProps>) => {
  const href = useHref(props.eventKey);

  return (
    <Tab href={href} {...props}>
      {children}
    </Tab>
  );
};

export const useRoutableTab = (to: Partial<Path>) => ({
  eventKey: to.pathname ?? "",
  href: useHref(to),
});
