import { Trans } from "react-i18next";

import { Permission } from "../api/representations";

type SharedWithProps = {
  permissions?: Permission[];
};

export const SharedWith = ({ permissions: p = [] }: SharedWithProps) => {
  return (
    <Trans i18nKey="resourceSharedWith" count={p.length}>
      <strong>
        {{
          username: p[0] ? p[0].username : undefined,
        }}
      </strong>
      <strong>
        {{
          other: p.length - 1,
        }}
      </strong>
    </Trans>
  );
};
