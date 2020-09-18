import React from "react";
import { useTranslation } from "react-i18next";

import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
  IFormatter,
  IFormatterValueType,
} from "@patternfly/react-table";

import { ExternalLink } from "../components/external-link/ExternalLink";
import { RoleRepresentation } from "../model/role-model";

type RolesListProps = {
  roles?: RoleRepresentation[];
};

const columns: (keyof RoleRepresentation)[] = [
  "name",
  "composite",
  "description",
];

export const RolesList = ({ roles }: RolesListProps) => {
  const { t } = useTranslation("roles");

  const emptyFormatter = (): IFormatter => (data?: IFormatterValueType) => {
    return data ? data : "—";
  };

  const externalLink = (): IFormatter => (data?: IFormatterValueType) => {
    return (data ? (
      <ExternalLink href={data.toString()} />
    ) : undefined) as object;
  };

  const boolFormatter = (): IFormatter => (data?: IFormatterValueType) => {
    const boolVal = data?.toString();

    return (boolVal
      ? boolVal.charAt(0).toUpperCase() + boolVal.slice(1)
      : undefined) as string;
  };

  const data = roles!.map((c) => {
    return { cells: columns.map((col) => c[col]) };
  });
  return (
    <Table
      variant={TableVariant.compact}
      cells={[
        {
          title: t("roleName"),
          cellFormatters: [externalLink(), emptyFormatter()],
        },
        {
          title: t("composite"),
          cellFormatters: [boolFormatter(), emptyFormatter()],
        },
        { title: t("description"), cellFormatters: [emptyFormatter()] },
      ]}
      rows={data}
      actions={[
        {
          title: t("common:Export"),
        },
        {
          title: t("common:Delete"),
        },
      ]}
      aria-label="Roles list"
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
