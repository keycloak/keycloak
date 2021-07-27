import React from "react";
import { Tooltip } from "@patternfly/react-core";
import type { TableTextProps } from "@patternfly/react-table";

import type GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";

type GroupPathProps = TableTextProps & {
  group: GroupRepresentation;
};

const MAX_LENGTH = 20;
const PART = 10;

const truncatePath = (path?: string) => {
  if (path && path.length >= MAX_LENGTH) {
    return (
      path.substr(0, PART) +
      "..." +
      path.substr(path.length - PART, path.length)
    );
  }
  return path;
};

export const GroupPath = ({
  group: { path },
  onMouseEnter: onMouseEnterProp,
  ...props
}: GroupPathProps) => {
  const [tooltip, setTooltip] = React.useState("");
  const onMouseEnter = (event: any) => {
    setTooltip(path!);
    onMouseEnterProp?.(event);
  };
  const text = (
    <span onMouseEnter={onMouseEnter} {...props}>
      {truncatePath(path)}
    </span>
  );

  return tooltip !== "" ? (
    <Tooltip content={tooltip} isVisible>
      {text}
    </Tooltip>
  ) : (
    text
  );
};
