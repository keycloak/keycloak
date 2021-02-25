import React, { useEffect, useState } from "react";
import { useHistory, useParams, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  PageSection,
} from "@patternfly/react-core";
import RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { formattedLinkTableCell } from "../components/external-link/FormattedLink";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import { AssociatedRolesModal } from "./AssociatedRolesModal";
import { useAdminClient } from "../context/auth/AdminClient";
import { RoleFormType } from "./RealmRoleTabs";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { AliasRendererComponent } from "./AliasRendererComponent";

type AssociatedRolesTabProps = {
  additionalRoles: RoleRepresentation[];
  addComposites: (newReps: RoleRepresentation[]) => void;
  parentRole: RoleFormType;
  onRemove: (newReps: RoleRepresentation[]) => void;
  client?: ClientRepresentation;
};

export const AssociatedRolesTab = ({
  additionalRoles,
  addComposites,
  parentRole,
  onRemove,
}: AssociatedRolesTabProps) => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const { addAlert } = useAlerts();
  const { url } = useRouteMatch();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [selectedRows, setSelectedRows] = useState<RoleRepresentation[]>([]);
  const [isInheritedHidden, setIsInheritedHidden] = useState(false);

  const [open, setOpen] = useState(false);

  const adminClient = useAdminClient();
  const { id } = useParams<{ id: string }>();
  const inheritanceMap = React.useRef<{ [key: string]: string }>({});

  const getSubRoles = async (
    role: RoleRepresentation,
    allRoles: RoleRepresentation[]
  ): Promise<RoleRepresentation[]> => {
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
    if (isInheritedHidden) {
      return additionalRoles;
    }

    const allRoles: Promise<RoleRepresentation[]> = additionalRoles.reduce(
      async (acc: Promise<RoleRepresentation[]>, role) => {
        const resolvedRoles = await acc;
        resolvedRoles.push(role);
        const subRoles = await getSubRoles(role, resolvedRoles);
        resolvedRoles.push(...subRoles);
        return acc;
      },
      Promise.resolve([] as RoleRepresentation[])
    );

    return allRoles;
  };

  useEffect(() => {
    refresh();
  }, [additionalRoles, isInheritedHidden]);

  const InheritedRoleName = (role: RoleRepresentation) => {
    return <>{inheritanceMap.current[role.id!]}</>;
  };

  const AliasRenderer = (role: RoleRepresentation) => {
    return (
      <>
        <AliasRendererComponent
          id={id}
          name={role.name}
          adminClient={adminClient}
          containerId={role.containerId}
        />
      </>
    );
  };

  console.log(inheritanceMap);

  const toggleModal = () => setOpen(!open);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleRemoveAssociatedRoleConfirm",
    messageKey: t("roles:roleRemoveAssociatedText"),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.roles.delCompositeRoles({ id }, selectedRows);
        setSelectedRows([]);

        addAlert(t("associatedRolesRemoved"), AlertVariant.success);
      } catch (error) {
        addAlert(t("roleDeleteError", { error }), AlertVariant.danger);
      }
    },
  });

  const [
    toggleDeleteAssociatedRolesDialog,
    DeleteAssociatedRolesConfirm,
  ] = useConfirmDialog({
    titleKey: t("roles:removeAssociatedRoles") + "?",
    messageKey: t("roles:removeAllAssociatedRolesConfirmDialog", {
      name: parentRole?.name || t("createRole"),
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        if (selectedRows.length === additionalRoles.length) {
          onRemove(selectedRows);
          const loc = url.replace(/\/AssociatedRoles/g, "/details");
          history.push(loc);
        }
        onRemove(selectedRows);
        await adminClient.roles.delCompositeRoles({ id }, selectedRows);
        addAlert(t("associatedRolesRemoved"), AlertVariant.success);
      } catch (error) {
        addAlert(`${t("roleDeleteError")} ${error}`, AlertVariant.danger);
      }
    },
  });

  const goToCreate = () => history.push(`${url}/add-role`);
  return (
    <>
      <PageSection variant="light">
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
          isPaginated
          toolbarItem={
            <>
              <Checkbox
                label="Hide inherited roles"
                key="associated-roles-check"
                id="kc-hide-inherited-roles-checkbox"
                onChange={() => setIsInheritedHidden(!isInheritedHidden)}
                isChecked={isInheritedHidden}
              />
              <Button
                className="kc-add-role-button"
                key="add-role-button"
                onClick={() => toggleModal()}
                data-cy="add-role-button"
              >
                {t("addRole")}
              </Button>
              <Button
                variant="link"
                isDisabled={selectedRows.length == 0}
                key="remove-role-button"
                onClick={() => {
                  toggleDeleteAssociatedRolesDialog();
                }}
              >
                {t("removeRoles")}
              </Button>
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
              cellFormatters: [formattedLinkTableCell(), emptyFormatter()],
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
              message={t("noRolesInThisRealm")}
              instructions={t("noRolesInThisRealmInstructions")}
              primaryActionText={t("createRole")}
              onPrimaryAction={goToCreate}
            />
          }
        />
      </PageSection>
    </>
  );
};
