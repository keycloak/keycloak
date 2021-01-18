import React, { Fragment, useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import { Badge, Button, Checkbox, ToolbarItem } from "@patternfly/react-core";

import { useAdminClient } from "../../context/auth/AdminClient";
import { DataLoader } from "../../components/data-loader/DataLoader";
import { TableToolbar } from "../../components/table-toolbar/TableToolbar";
import { RealmContext } from "../../context/realm-context/RealmContext";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { emptyFormatter } from "../../util";

import "./service-account.css";

type ServiceAccountProps = {
  clientId: string;
};

type CompositeRole = RoleRepresentation & {
  parent: RoleRepresentation;
};

export const ServiceAccount = ({ clientId }: ServiceAccountProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { realm } = useContext(RealmContext);

  const [hide, setHide] = useState(false);

  const loader = async () => {
    const serviceAccount = await adminClient.clients.getServiceAccountUser({
      id: clientId,
    });
    const effectiveRoles = await adminClient.users.listCompositeRealmRoleMappings(
      { id: serviceAccount.id! }
    );
    const assignedRoles = await adminClient.users.listRealmRoleMappings({
      id: serviceAccount.id!,
    });

    const clients = await adminClient.clients.find();
    const clientRoles = (
      await Promise.all(
        clients.map(async (client) => {
          return {
            client,
            roles: await adminClient.users.listClientRoleMappings({
              id: serviceAccount.id!,
              clientUniqueId: client.id!,
            }),
          };
        })
      )
    ).filter((rows) => rows.roles.length > 0);

    const findClient = (role: RoleRepresentation) => {
      const row = clientRoles.filter((row) =>
        row.roles.find((r) => r.id === role.id)
      )[0];
      return row ? row.client : undefined;
    };

    const clientRolesFlat = clientRoles.map((row) => row.roles).flat();

    const addInherentData = await (async () =>
      Promise.all(
        effectiveRoles.map(async (role) => {
          const compositeRoles = await adminClient.roles.getCompositeRolesForRealm(
            { realm, id: role.id! }
          );
          return compositeRoles.length > 0
            ? compositeRoles.map((r) => {
                return { ...r, parent: role };
              })
            : { ...role, parent: undefined };
        })
      ))();
    const uniqueRolesWithParent = addInherentData
      .flat()
      .filter(
        (role, index, array) =>
          array.findIndex((r) => r.id === role.id) === index
      );
    return ([
      ...(hide ? assignedRoles : uniqueRolesWithParent),
      ...clientRolesFlat,
    ] as CompositeRole[])
      .sort((r1, r2) => r1.name!.localeCompare(r2.name!))
      .map((role) => {
        const client = findClient(role);
        return {
          cells: [
            <Fragment key={role.id}>
              {client && (
                <Badge
                  key={client.id}
                  isRead
                  className="keycloak-admin--service-account__client-name"
                >
                  {client.clientId}
                </Badge>
              )}
              {role.name}
            </Fragment>,
            role.parent ? role.parent.name : "",
            role.description,
          ],
        };
      });
  };

  const filterData = () => {};

  return (
    <TableToolbar
      inputGroupName="clientsServiceAccountRoleToolbarTextInput"
      inputGroupPlaceholder={t("searchByName")}
      inputGroupOnChange={filterData}
      toolbarItem={
        <>
          <ToolbarItem>
            <Checkbox
              label={t("hideInheritedRoles")}
              id="hideInheritedRoles"
              isChecked={hide}
              onChange={setHide}
            />
          </ToolbarItem>
          <ToolbarItem>
            <Button>{t("assignRole")}</Button>
          </ToolbarItem>
        </>
      }
    >
      <DataLoader loader={loader} deps={[clientId]}>
        {(clientRoles) => (
          <>
            {hide ? "" : " "}
            <Table
              onSelect={() => {}}
              variant={TableVariant.compact}
              cells={[
                t("roles:roleName"),
                {
                  title: t("inherentFrom"),
                  cellFormatters: [emptyFormatter()],
                },
                {
                  title: t("common:description"),
                  cellFormatters: [emptyFormatter()],
                },
              ]}
              rows={clientRoles}
              aria-label="roleList"
            >
              <TableHeader />
              <TableBody />
            </Table>
          </>
        )}
      </DataLoader>
    </TableToolbar>
  );
};
