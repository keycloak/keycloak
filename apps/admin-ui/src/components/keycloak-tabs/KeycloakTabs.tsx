import { Children, isValidElement, useState } from "react";
import { useRouteMatch } from "react-router-dom";
import { useNavigate } from "react-router-dom-v5-compat";
import { TabProps, Tabs, TabsProps } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";

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
    if (url.includes(key)) {
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
  const navigate = useNavigate();
  const form = useFormContext() as
    | ReturnType<typeof useFormContext>
    | undefined;
  const [key, setKey] = useState("");

  const firstTab = Children.toArray(children)[0];
  const tab =
    params[paramName] ||
    (isValidElement<TabProps>(firstTab) && firstTab.props.eventKey) ||
    "";

  const pathIndex = match.path.indexOf(paramName) + paramName.length;
  const path = match.path.substr(0, pathIndex);

  const [toggleChangeTabDialog, ChangeTabConfirm] = useConfirmDialog({
    titleKey: "common:leaveDirtyTitle",
    messageKey: "common:leaveDirtyConfirm",
    continueButtonLabel: "common:leave",
    onConfirm: () => {
      form?.reset();
      navigate(createUrl(path, { ...params, [paramName]: key as string }));
    },
  });

  return (
    <>
      <ChangeTabConfirm />
      <Tabs
        inset={{
          default: "insetNone",
          md: "insetSm",
          xl: "inset2xl",
          "2xl": "insetLg",
        }}
        activeKey={tab}
        onSelect={(_, key) => {
          if (form?.formState.isDirty) {
            setKey(key as string);
            toggleChangeTabDialog();
          } else {
            navigate(
              createUrl(path, { ...params, [paramName]: key as string })
            );
          }
        }}
        {...rest}
      >
        {children}
      </Tabs>
    </>
  );
};
