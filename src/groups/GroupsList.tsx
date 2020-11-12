import React, { useState, useEffect } from "react";
import {
  Table,
  TableHeader,
  TableBody,
  TableVariant,
} from "@patternfly/react-table";
import { Button, AlertVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { GroupRepresentation } from "./models/groups";
import { UsersIcon } from "@patternfly/react-icons";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";

export type GroupsListProps = {
  list?: GroupRepresentation[];
  refresh: () => void;
  tableRowSelectedArray: number[];
  setTableRowSelectedArray: (tableRowSelectedArray: number[]) => void;
};

type FormattedData = {
  cells: JSX.Element[];
  selected: boolean;
};

export const GroupsList = ({
  list,
  refresh,
  tableRowSelectedArray,
  setTableRowSelectedArray,
}: GroupsListProps) => {
  const { t } = useTranslation("groups");
  const adminClient = useAdminClient();
  const columnGroupName: keyof GroupRepresentation = "name";
  const columnGroupNumber: keyof GroupRepresentation = "membersLength";
  const { addAlert } = useAlerts();
  const [formattedData, setFormattedData] = useState<FormattedData[]>([]);

  const formatData = (data: GroupRepresentation[]) =>
    data.map((group: { [key: string]: any }, index) => {
      const groupName = group[columnGroupName];
      const groupNumber = group[columnGroupNumber];
      return {
        cells: [
          <Button variant="link" key={index}>
            {groupName}
          </Button>,
          <div className="keycloak-admin--groups__member-count" key={index}>
            <UsersIcon key={`user-icon-${index}`} />
            {groupNumber}
          </div>,
        ],
        selected: false,
      };
    });

  useEffect(() => {
    setFormattedData(formatData(list!));
  }, [list]);

  function onSelect(
    _: React.FormEvent<HTMLInputElement>,
    isSelected: boolean,
    rowId: number
  ) {
    let localRow;
    if (rowId === undefined) {
      localRow = formattedData.map((row: { [key: string]: any }) => {
        row.selected = isSelected;
        return row;
      });
    } else {
      localRow = [...formattedData];
      const localTableRow = [...tableRowSelectedArray];
      if (localRow[rowId].selected !== isSelected) {
        localRow[rowId].selected = isSelected;
      }

      if (localTableRow.includes(rowId)) {
        const index = localTableRow.indexOf(rowId);
        if (index === 0) {
          localTableRow.shift();
        } else {
          localTableRow.splice(index, 1);
        }
        setTableRowSelectedArray(localTableRow);
      } else {
        setTableRowSelectedArray([rowId, ...tableRowSelectedArray]);
      }
      setFormattedData(localRow);
    }
  }

  const tableHeader = [{ title: t("groupName") }, { title: t("members") }];
  const actions = [
    {
      title: t("moveTo"),
      onClick: () => console.log("TO DO: Add move to functionality"),
    },
    {
      title: t("common:Delete"),
      onClick: async (
        _: React.MouseEvent<Element, MouseEvent>,
        rowId: number
      ) => {
        try {
          await adminClient.groups.del({ id: list![rowId].id! });
          refresh();
          setTableRowSelectedArray([]);
          addAlert(t("Group deleted"), AlertVariant.success);
        } catch (error) {
          addAlert(`${t("clientDeleteError")} ${error}`, AlertVariant.danger);
        }
      },
    },
  ];

  return (
    <>
      {formattedData && (
        <Table
          actions={actions}
          variant={TableVariant.compact}
          onSelect={onSelect}
          canSelectAll={false}
          aria-label={t("tableOfGroups")}
          cells={tableHeader}
          rows={formattedData}
        >
          <TableHeader />
          <TableBody />
        </Table>
      )}
    </>
  );
};
