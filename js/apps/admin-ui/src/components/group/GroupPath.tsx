import { useState } from "react";
import { Tooltip } from "@patternfly/react-core";
import type { TableTextProps } from "@patternfly/react-table";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";

type GroupPathProps = TableTextProps & {
  group: GroupRepresentation;
};

export const GroupPath = ({
  group: { path },
  onMouseEnter: onMouseEnterProp,
  ...props
}: GroupPathProps) => {
  const [tooltip, setTooltip] = useState("");
  const onMouseEnter = (event: any) => {
    setTooltip(path!);
    onMouseEnterProp?.(event);
  };
  const text = (
    <span onMouseEnter={onMouseEnter} {...props}>
      {path}
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
