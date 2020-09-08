import React from "react";
import { Tooltip } from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

type HelpItemProps = {
  item: string;
};

export const HelpItem = ({ item }: HelpItemProps) => {
  const { t } = useTranslation();
  return (
    <Tooltip position="right" content={t(`help:${item}`)}>
      <span id={item} data-testid={item}>
        <HelpIcon />
      </span>
    </Tooltip>
  );
};
