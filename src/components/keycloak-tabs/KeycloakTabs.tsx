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
    } else {
      url += `/${value}`;
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

  return (
    <Tabs
      activeKey={tab}
      onSelect={(_, key) =>
        history.push(
          createUrl(match.path, { ...params, [paramName]: key as string })
        )
      }
      {...rest}
    >
      {children}
    </Tabs>
  );
};
