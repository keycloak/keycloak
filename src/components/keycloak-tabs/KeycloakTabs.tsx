import React, { Children, isValidElement } from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
import { TabProps, Tabs, TabsProps } from "@patternfly/react-core";

type KeycloakTabsProps = Omit<TabsProps, "ref" | "activeKey" | "onSelect"> & {
  paramName?: string;
};

const createUrl = (
  path: string,
  params: { [index: string]: string }
): string => {
  let url = path;
  for (const key in params) {
    const value = params[key];
    if (url.indexOf(key) !== -1) {
      url = url.replace(new RegExp(`:${key}\\??`), value || "");
    }
  }
  return url;
};

export const KeycloakTabs = ({
  paramName = "tab",
  children,
  ...rest
}: KeycloakTabsProps) => {
  const match = useRouteMatch();
  const params = match.params as { [index: string]: string };
  const history = useHistory();

  const firstTab = Children.toArray(children)[0];
  const tab =
    params[paramName] ||
    (isValidElement<TabProps>(firstTab) && firstTab.props.eventKey) ||
    "";

  const pathIndex = match.path.indexOf(paramName) + paramName.length;
  const path = match.path.substr(0, pathIndex);
  return (
    <Tabs
      inset={{
        default: "insetNone",
        md: "insetSm",
        xl: "inset2xl",
        "2xl": "insetLg",
      }}
      activeKey={tab}
      onSelect={(_, key) =>
        history.push(createUrl(path, { ...params, [paramName]: key as string }))
      }
      {...rest}
    >
      {children}
    </Tabs>
  );
};
