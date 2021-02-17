import React, { useState } from "react";
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
import { boolFormatter, emptyFormatter } from "../util";
import { AssociatedRolesModal } from "./AssociatedRolesModal";
import { useAdminClient } from "../context/auth/AdminClient";
import { RoleFormType } from "./RealmRoleTabs";

type AssociatedRolesTabProps = {
  additionalRoles: RoleRepresentation[];
  addComposites: (newReps: RoleRepresentation[]) => void;
  parentRole: RoleFormType;
  onRemove: (newReps: RoleRepresentation[]) => void;
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
  const tableRefresher = React.useRef<() => void>();

  const [selectedRows, setSelectedRows] = useState<RoleRepresentation[]>([]);
  const [open, setOpen] = useState(false);

  const adminClient = useAdminClient();
  const { id } = useParams<{ id: string; clientId: string }>();

  const loader = async () => {
    return Promise.resolve(additionalRoles);
  };

  React.useEffect(() => {
    tableRefresher.current && tableRefresher.current();
  }, [additionalRoles]);

  const RoleName = (role: RoleRepresentation) => <>{role.name}</>;

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

  const setRefresher = (refresher: () => void) => {
    tableRefresher.current = refresher;
  };

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
          loader={loader}
          ariaLabelKey="roles:roleList"
          searchPlaceholderKey="roles:searchFor"
          canSelectAll
          onSelect={(rows) => {
            setSelectedRows([...rows]);
          }}
          isPaginated
          setRefresher={setRefresher}
          toolbarItem={
            <>
              <Checkbox
                label="Hide inherited roles"
                key="associated-roles-check"
                id="kc-hide-inherited-roles-checkbox"
              />
              <Button
                className="kc-add-role-button"
                key="add-role-button"
                onClick={() => toggleModal()}
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
              cellRenderer: RoleName,
              cellFormatters: [formattedLinkTableCell(), emptyFormatter()],
            },
            {
              name: "inherited from",
              displayKey: "roles:inheritedFrom",
              cellFormatters: [boolFormatter(), emptyFormatter()],
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
