import {
  TabProps,
  Tabs,
  TabsComponent,
  TabsProps,
} from "@patternfly/react-core";
import type { History, LocationDescriptorObject } from "history";
import React, {
  Children,
  isValidElement,
  JSXElementConstructor,
  ReactElement,
} from "react";
import { useLocation } from "react-router-dom";

// TODO: Remove the custom 'children' props and type once the following issue has been resolved:
// https://github.com/patternfly/patternfly-react/issues/6766
type ChildElement = ReactElement<TabProps, JSXElementConstructor<TabProps>>;
type Child = ChildElement | boolean | null | undefined;

// TODO: Figure out why we need to omit 'ref' from the props.
type RoutableTabsProps = {
  children: Child | Child[];
  defaultLocation?: LocationDescriptorObject;
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

  // Extract event keys from children.
  const eventKeys = Children.toArray(children)
    .filter((child): child is ChildElement => isValidElement(child))
    .map((child) => child.props.eventKey.toString());

  // Determine if there is an exact match.
  const exactMatch = eventKeys.find((eventKey) => eventKey === pathname);

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
        md: "insetSm",
        xl: "inset2xl",
        "2xl": "insetLg",
      }}
      {...otherProps}
    >
      {children}
    </Tabs>
  );
};

type RoutableTabParams = {
  to: LocationDescriptorObject;
  history: History<unknown>;
};

export const routableTab = ({ to, history }: RoutableTabParams) => ({
  eventKey: to.pathname ?? "",
  href: history.createHref(to),
});
