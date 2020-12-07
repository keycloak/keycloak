import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
  IFormatter,
  IFormatterValueType,
} from "@patternfly/react-table";
import { AlertVariant, ButtonVariant } from "@patternfly/react-core";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";

import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";

type RolesListProps = {
  roles?: RoleRepresentation[];
  refresh: () => void;
};

const columns: (keyof RoleRepresentation)[] = [
  "name",
  "composite",
  "description",
];

export const RolesList = ({ roles, refresh }: RolesListProps) => {
  const data1 = roles?.map((c) => {
    return {
      cells: columns.map((col) => {
        if (col === "name") {
          return (
            <>
              <Link key={c.id} to={`/roles/${c.id}`}>
                {c[col]}
              </Link>
            </>
          );
        }
        return c[col];
      }),
    };
  });

  const { t } = useTranslation("roles");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const [selectedRowId, setSelectedRowId] = useState(-1);

  const boolFormatter = (): IFormatter => (data?: IFormatterValueType) => {
    const boolVal = data?.toString();

    return (boolVal
      ? boolVal.charAt(0).toUpperCase() + boolVal.slice(1)
      : undefined) as string;
  };
  const data = roles!.map((column) => {
    return { cells: columns.map((col) => column[col]), role: column };
  });

  let selectedRoleName;
  if (selectedRowId === data.length) {
    selectedRoleName = data[selectedRowId - 1].role.name;
  } else if (selectedRowId != -1) {
    selectedRoleName = data[selectedRowId].role.name;
  }

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleDeleteConfirm",
    messageKey: t("roles:roleDeleteConfirmDialog", { selectedRoleName }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.roles.delByName({
          name: data[selectedRowId].role.name!,
        });
        refresh();
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(`${t("roleDeleteError")} ${error}`, AlertVariant.danger);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <Table
        variant={TableVariant.compact}
        cells={[
          {
            title: t("roleName"),
            cellFormatters: [emptyFormatter()],
          },
          {
            title: t("composite"),
            cellFormatters: [boolFormatter(), emptyFormatter()],
          },
          { title: t("description"), cellFormatters: [emptyFormatter()] },
        ]}
        rows={data1}
        actions={[
          {
            title: t("common:Delete"),
            onClick: (_, rowId) => {
              setSelectedRowId(rowId);
              toggleDeleteDialog();
            },
          },
        ]}
        aria-label="Roles list"
      >
        <TableHeader />
        <TableBody />
      </Table>
    </>
  );
};
