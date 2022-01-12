import React from "react";
import { useTranslation } from "react-i18next";
import {
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
} from "@patternfly/react-core";

type DetailDescriptionProps<T> = {
  name: string;
  array?: string[] | T[];
  convert?: (obj: T) => string;
};

export function DetailDescription<T>({
  name,
  array,
  convert,
}: DetailDescriptionProps<T>) {
  const { t } = useTranslation("clients");
  return (
    <DescriptionListGroup>
      <DescriptionListTerm>{t(name)}</DescriptionListTerm>
      <DescriptionListDescription>
        {array?.map((element) => {
          const value =
            typeof element === "string" ? element : convert!(element);
          return (
            <span key={value} className="pf-u-pr-sm">
              {value}
            </span>
          );
        })}
        {array?.length === 0 && <i>{t("common:none")}</i>}
      </DescriptionListDescription>
    </DescriptionListGroup>
  );
}
