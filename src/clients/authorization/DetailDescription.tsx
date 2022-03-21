import React from "react";
import { Link } from "react-router-dom";
import type { LocationDescriptor } from "history";
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

export function DetailDescription<T>(props: DetailDescriptionProps<T>) {
  return <DetailDescriptionLink {...props} />;
}

type DetailDescriptionLinkProps<T> = DetailDescriptionProps<T> & {
  link?: (element: T) => LocationDescriptor;
};

export function DetailDescriptionLink<T>({
  name,
  array,
  convert,
  link,
}: DetailDescriptionLinkProps<T>) {
  const { t } = useTranslation("clients");
  return (
    <DescriptionListGroup>
      <DescriptionListTerm>{t(name)}</DescriptionListTerm>
      <DescriptionListDescription>
        {array?.map((element) => {
          const value =
            typeof element === "string" ? element : convert!(element);
          return link ? (
            <Link key={value} to={link(element as T)} className="pf-u-pr-sm">
              {value}
            </Link>
          ) : (
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
