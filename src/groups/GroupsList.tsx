import React, { useState, useEffect, useContext } from "react";
import {
  Table,
  TableHeader,
  TableBody,
  TableVariant,
} from "@patternfly/react-table";
import { Button } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { GroupRepresentation } from "./models/groups";
import { UsersIcon } from "@patternfly/react-icons";
import { HttpClientContext } from "../context/http-service/HttpClientContext";

type GroupsListProps = {
  list?: GroupRepresentation[];
};

export const GroupsList = ({ list }: GroupsListProps) => {
  const { t } = useTranslation("groups");
  const httpClient = useContext(HttpClientContext)!;
  const columnGroupName: keyof GroupRepresentation = "name";
  const columnGroupNumber: keyof GroupRepresentation = "membersLength";
  const [formattedData, setFormattedData] = useState([
    { cells: [<Button key="0">Test</Button>], selected: false },
  ]);

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
            <UsersIcon />
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
    event: React.FormEvent<HTMLInputElement>,
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
      localRow[rowId].selected = isSelected;
      setFormattedData(localRow);
    }
  }

  // Delete individual rows using the action in the table
  function onDelete(rowIndex: number) {
    const localFilteredData = [...list!];
    httpClient.doDelete(
      `/admin/realms/master/groups/${localFilteredData[rowIndex].id}`
    );
    // TO DO update the state
  }

  const tableHeader = [{ title: t("groupName") }, { title: t("members") }];
  const actions = [
    {
      title: t("moveTo"),
      onClick: () => console.log("TO DO: Add move to functionality"),
    },
    {
      title: t("common:Delete"),
      onClick: () => onDelete,
    },
  ];

  return (
    <React.Fragment>
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
    </React.Fragment>
  );
};
