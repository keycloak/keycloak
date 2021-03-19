import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import { Badge, Button, Checkbox, ToolbarItem } from "@patternfly/react-core";

import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { emptyFormatter } from "../../util";

import "./service-account.css";

type ServiceAccountProps = {
  clientId: string;
};

type Row = {
  client: ClientRepresentation;
  role: CompositeRole;
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
    console.log(clientRolesFlat);

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
        return {
          client: findClient(role),
          role,
        } as Row;
      });
  };

  const RoleLink = ({ role, client }: Row) => (
    <>
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
    </>
  );

  return (
    <KeycloakDataTable
      loader={loader}
      onSelect={() => {}}
      searchPlaceholderKey="clients:searchByName"
      ariaLabelKey="clients:clientScopeList"
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
      columns={[
        {
          name: "role.name",
          displayKey: t("name"),
          cellRenderer: RoleLink,
        },
        {
          name: "role.parent.name",
          displayKey: t("inherentFrom"),
          cellFormatters: [emptyFormatter()],
        },
        {
          name: "role.description",
          displayKey: t("description"),
          cellFormatters: [emptyFormatter()],
        },
      ]}
    />
  );
};
