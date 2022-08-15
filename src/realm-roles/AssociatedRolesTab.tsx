import { useState } from "react";
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

import {
  ClientRoleParams,
  ClientRoleRoute,
  toClientRole,
} from "./routes/ClientRole";
import {
  RealmSettingsParams,
  RealmSettingsRoute,
} from "../realm-settings/routes/RealmSettings";
import { RealmRoleParams, RealmRoleTab, toRealmRole } from "./routes/RealmRole";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter } from "../util";
import { AddRoleMappingModal } from "../components/role-mapping/AddRoleMappingModal";
import { useAdminClient } from "../context/auth/AdminClient";
import type { AttributeForm } from "../components/key-value-form/AttributeForm";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";

type AssociatedRolesTabProps = {
  parentRole: AttributeForm;
  client?: ClientRepresentation;
  refresh: () => void;
};

type Role = RoleRepresentation & {
  inherited?: string;
};

export const AssociatedRolesTab = ({
  parentRole,
  refresh: refreshParent,
}: AssociatedRolesTabProps) => {
  const { t } = useTranslation("roles");

  const history = useHistory();
  const { addAlert, addError } = useAlerts();
  const { id, realm } = useParams<RealmRoleParams>();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [selectedRows, setSelectedRows] = useState<RoleRepresentation[]>([]);
  const [isInheritedHidden, setIsInheritedHidden] = useState(false);
  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);

  const { adminClient } = useAdminClient();
  const clientRoleRouteMatch = useRouteMatch<ClientRoleParams>(
    ClientRoleRoute.path
  );

  const realmSettingsMatch = useRouteMatch<RealmSettingsParams>(
    RealmSettingsRoute.path
  );

  const subRoles = async (result: Role[], roles: Role[]): Promise<Role[]> => {
    const promises = roles.map(async (r) => {
      if (result.find((o) => o.id === r.id)) return result;
      result.push(r);
      if (r.composite) {
        const subList = (await adminClient.roles.getCompositeRoles({
          id: r.id!,
        })) as Role[];
        subList.map((o) => (o.inherited = r.name));
        result.concat(await subRoles(result, subList));
      }
      return result;
    });
    await Promise.all(promises);
    return [...result];
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    const compositeRoles = await adminClient.roles.getCompositeRoles({
      id: parentRole.id!,
      first: first,
      max: max!,
      search: search,
    });
    setCount(compositeRoles.length);

    if (!isInheritedHidden) {
      const children = await subRoles([], compositeRoles);
      compositeRoles.splice(0, compositeRoles.length);
      compositeRoles.push(...children);
    }

    await Promise.all(
      compositeRoles.map(async (role) => {
        if (role.clientRole) {
          role.containerId = (
            await adminClient.clients.findOne({
              id: role.containerId!,
            })
          )?.clientId;
        }
      })
    );

    return compositeRoles;
  };

  const toRolesTab = (tab: RealmRoleTab | undefined = "associated-roles") => {
    const to = clientRoleRouteMatch
      ? toClientRole({ ...clientRoleRouteMatch.params, tab })
      : !realmSettingsMatch
      ? toRealmRole({
          realm,
          id,
          tab,
        })
      : undefined;
    if (to) history.push(to);
  };

  const AliasRenderer = ({ id, name, clientRole, containerId }: Role) => {
    return (
      <>
        {clientRole && (
          <Label color="blue" key={`label-${id}`}>
            {containerId}
          </Label>
        )}{" "}
        {name}
      </>
    );
  };

  const toggleModal = () => {
    setOpen(!open);
  };

  const reload = () => {
    if (selectedRows.length >= count) {
      refreshParent();
      toRolesTab("details");
    } else {
      refresh();
    }
  };

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
        await adminClient.roles.delCompositeRoles(
          { id: parentRole.id! },
          selectedRows
        );
        reload();
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
        name: parentRole.name || t("createRole"),
      }),
      continueButtonLabel: "common:remove",
      continueButtonVariant: ButtonVariant.danger,
      onConfirm: async () => {
        try {
          await adminClient.roles.delCompositeRoles(
            { id: parentRole.id! },
            selectedRows
          );
          addAlert(t("associatedRolesRemoved"), AlertVariant.success);
          reload();
        } catch (error) {
          addError("roles:roleDeleteError", error);
        }
      },
    });

  const addComposites = async (composites: RoleRepresentation[]) => {
    const compositeArray = composites;

    try {
      await adminClient.roles.createComposite(
        { roleId: parentRole.id!, realm },
        compositeArray
      );
      toRolesTab();
      refresh();
      addAlert(t("addAssociatedRolesSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roles:addAssociatedRolesError", error);
    }
  };

  return (
    <PageSection variant="light" padding={{ default: "noPadding" }}>
      <DeleteConfirm />
      <DeleteAssociatedRolesConfirm />
      {open && (
        <AddRoleMappingModal
          id={parentRole.id!}
          type="roles"
          name={parentRole.name}
          onAssign={(rows) => addComposites(rows.map((r) => r.role))}
          onClose={() => setOpen(false)}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="roles:roleList"
        searchPlaceholderKey="roles:searchFor"
        canSelectAll
        isPaginated
        onSelect={(rows) => {
          setSelectedRows([
            ...rows.map((r) => {
              delete r.inherited;
              return r;
            }),
          ]);
        }}
        toolbarItem={
          <>
            <ToolbarItem>
              <Checkbox
                label="Hide inherited roles"
                key="associated-roles-check"
                id="kc-hide-inherited-roles-checkbox"
                onChange={() => {
                  setIsInheritedHidden(!isInheritedHidden);
                  refresh();
                }}
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
                data-testid="removeRoles"
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
            name: "inherited",
            displayKey: "roles:inheritedFrom",
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
            hasIcon
            message={t("noRolesAssociated")}
            instructions={t("noRolesAssociatedInstructions")}
            primaryActionText={t("addRole")}
            onPrimaryAction={() => setOpen(true)}
          />
        }
      />
    </PageSection>
  );
};
