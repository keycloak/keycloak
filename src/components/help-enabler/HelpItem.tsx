import React, { useContext } from "react";
import { Tooltip } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { HelpContext } from "./HelpHeader";

type HelpItemProps = {
  item: string;
};

export const HelpItem = ({ item }: HelpItemProps) => {
  const { t } = useTranslation();
  const { enabled } = useContext(HelpContext);
  return (
    <>
      {enabled && (
        <Tooltip position="right" content={t(`help:${item}`)}>
          <span id={item} data-testid={item}>
            <HelpIcon />
          </span>
        </Tooltip>
      )}
    </>
  );
};
