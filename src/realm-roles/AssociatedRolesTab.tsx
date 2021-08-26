import React, { useEffect, useState } from "react";
import { useHistory, useParams, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  Label,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import { AssociatedRolesModal } from "./AssociatedRolesModal";
import { useAdminClient } from "../context/auth/AdminClient";
import type { RoleFormType } from "./RealmRoleTabs";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import _ from "lodash";

type AssociatedRolesTabProps = {
  additionalRoles: Role[];
  addComposites: (newReps: RoleRepresentation[]) => void;
  parentRole: RoleFormType;
  onRemove: (newReps: RoleRepresentation[]) => void;
  client?: ClientRepresentation;
};

type Role = RoleRepresentation & {
  clientId?: string;
};

export const AssociatedRolesTab = ({
  additionalRoles,
  addComposites,
  parentRole,
  onRemove,
}: AssociatedRolesTabProps) => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const { addAlert, addError } = useAlerts();
  const { url } = useRouteMatch();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [selectedRows, setSelectedRows] = useState<RoleRepresentation[]>([]);
  const [isInheritedHidden, setIsInheritedHidden] = useState(false);
  const [allRoles, setAllRoles] = useState<RoleRepresentation[]>([]);

  const [open, setOpen] = useState(false);

  const adminClient = useAdminClient();
  const { id } = useParams<{ id: string }>();
  const inheritanceMap = React.useRef<{ [key: string]: string }>({});

  const getSubRoles = async (role: Role, allRoles: Role[]): Promise<Role[]> => {
    // Fetch all composite roles
    const allCompositeRoles = await adminClient.roles.getCompositeRoles({
      id: role.id!,
    });

    // Need to ensure we don't get into an infinite loop, do not add any role that is already there or the starting role
    const newRoles: Promise<RoleRepresentation[]> = allCompositeRoles.reduce(
      async (acc: Promise<RoleRepresentation[]>, newRole) => {
        const resolvedRoles = await acc;
        if (!allRoles.find((ar) => ar.id === newRole.id)) {
          inheritanceMap.current[newRole.id!] = role.name!;
          resolvedRoles.push(newRole);
          const subRoles = await getSubRoles(newRole, [
            ...allRoles,
            ...resolvedRoles,
          ]);
          resolvedRoles.push(...subRoles);
        }

        return acc;
      },
      Promise.resolve([] as RoleRepresentation[])
    );

    return newRoles;
  };

  const loader = async () => {
    const alphabetize = (rolesList: Role[]) => {
      return _.sortBy(rolesList, (role) => role.name?.toUpperCase());
    };
    const clients = await adminClient.clients.find();

    if (isInheritedHidden) {
      setAllRoles(additionalRoles);
      return alphabetize(
        additionalRoles.filter(
          (role) =>
            role.containerId === "master" && !inheritanceMap.current[role.id!]
        )
      );
    }

    const fetchedRoles: Promise<Role[]> = additionalRoles.reduce(
      async (acc: Promise<Role[]>, role) => {
        const resolvedRoles = await acc;
        resolvedRoles.push(role);
        const subRoles = await getSubRoles(role, resolvedRoles);
        resolvedRoles.push(...subRoles);
        return acc;
      },
      Promise.resolve([] as Role[])
    );

    return fetchedRoles.then((results: Role[]) => {
      const filterDupes = results.filter(
        (thing, index, self) =>
          index === self.findIndex((t) => t.name === thing.name)
      );
      filterDupes
        .filter((role) => role.clientRole)
        .map(
          (role) =>
            (role.clientId = clients.find(
              (client) => client.id === role.containerId
            )!.clientId!)
        );

      return alphabetize(additionalRoles);
    });
  };

  useEffect(() => {
    refresh();
  }, [additionalRoles, isInheritedHidden]);

  const InheritedRoleName = (role: RoleRepresentation) =>
    inheritanceMap.current[role.id!];

  const AliasRenderer = ({ id, name, clientId }: Role) => {
    return (
      <>
        {clientId && (
          <Label color="blue" key={`label-${id}`}>
            {clientId}
          </Label>
        )}{" "}
        {name}
      </>
    );
  };

  const toggleModal = () => setOpen(!open);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleRemoveAssociatedRoleConfirm",
    messageKey: t("roles:roleRemoveAssociatedText", {
      role: selectedRows.map((r) => r.name),
      roleName: parentRole.name,
    }),
    continueButtonLabel: "common:remove",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.roles.delCompositeRoles({ id }, selectedRows);
        onRemove(selectedRows);
        setSelectedRows([]);

        addAlert(t("associatedRolesRemoved"), AlertVariant.success);
      } catch (error) {
        addError("roles:roleDeleteError", error);
      }
    },
  });

  const [toggleDeleteAssociatedRolesDialog, DeleteAssociatedRolesConfirm] =
    useConfirmDialog({
      titleKey: t("roles:removeAssociatedRoles") + "?",
      messageKey: t("roles:removeAllAssociatedRolesConfirmDialog", {
        name: parentRole?.name || t("createRole"),
      }),
      continueButtonLabel: "common:remove",
      continueButtonVariant: ButtonVariant.danger,
      onConfirm: async () => {
        try {
          if (selectedRows.length >= allRoles.length) {
            onRemove(selectedRows);
            const loc = url.replace(/\/AssociatedRoles/g, "/details");
            history.push(loc);
          }
          onRemove(selectedRows);
          await adminClient.roles.delCompositeRoles({ id }, selectedRows);
          addAlert(t("associatedRolesRemoved"), AlertVariant.success);
          refresh();
        } catch (error) {
          addError("roles:roleDeleteError", error);
        }
      },
    });

  const goToCreate = () => history.push(`${url}/add-role`);
  return (
    <PageSection variant="light" padding={{ default: "noPadding" }}>
      <DeleteConfirm />
      <DeleteAssociatedRolesConfirm />
      <AssociatedRolesModal
        onConfirm={addComposites}
        existingCompositeRoles={additionalRoles}
        open={open}
        toggleDialog={() => setOpen(!open)}
      />
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="roles:roleList"
        searchPlaceholderKey="roles:searchFor"
        canSelectAll
        onSelect={(rows) => {
          setSelectedRows([...rows]);
        }}
        toolbarItem={
          <>
            <ToolbarItem>
              <Checkbox
                label="Hide inherited roles"
                key="associated-roles-check"
                id="kc-hide-inherited-roles-checkbox"
                onChange={() => setIsInheritedHidden(!isInheritedHidden)}
                isChecked={isInheritedHidden}
              />
            </ToolbarItem>
            <ToolbarItem>
              <Button
                key="add-role-button"
                onClick={() => toggleModal()}
                data-testid="add-role-button"
              >
                {t("addRole")}
              </Button>
            </ToolbarItem>
            <ToolbarItem>
              <Button
                variant="link"
                isDisabled={selectedRows.length === 0}
                key="remove-role-button"
                onClick={() => {
                  toggleDeleteAssociatedRolesDialog();
                }}
              >
                {t("removeRoles")}
              </Button>
            </ToolbarItem>
          </>
        }
        actions={[
          {
            title: t("common:remove"),
            onRowClick: (role) => {
              setSelectedRows([role]);
              toggleDeleteDialog();
            },
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: "roles:roleName",
            cellRenderer: AliasRenderer,
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "containerId",
            displayKey: "roles:inheritedFrom",
            cellRenderer: InheritedRoleName,
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "description",
            displayKey: "common:description",
            cellFormatters: [emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            message={t("noRoles")}
            instructions={t("noRolesInstructions")}
            primaryActionText={t("createRole")}
            onPrimaryAction={goToCreate}
          />
        }
      />
    </PageSection>
  );
};
