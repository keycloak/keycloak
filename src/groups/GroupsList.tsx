import React from "react";
import {
  Table,
  TableHeader,
  TableBody,
  TableVariant,
} from "@patternfly/react-table";
import { GroupRepresentation } from "./models/groups";
import { useTranslation } from "react-i18next";

type GroupsListProps = {
  list: GroupRepresentation[];
};

export const GroupsList = ({ list }: GroupsListProps) => {
  const { t } = useTranslation("group");
  const columns: (keyof GroupRepresentation)[] = ["name"];

  const data = list.map((c) => {
    return { cells: columns.map((col) => c[col]) };
  });

  console.log(list);
  return (
    <Table
      aria-label="Simple Table"
      variant={TableVariant.compact}
      cells={[{ title: t("Name") }]}
      rows={data}
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
