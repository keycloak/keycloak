import { useState } from "react";
import { useHistory, useParams, useRouteMatch } from "react-router-dom";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { omit } from "lodash-es";

import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  AttributesForm,
  AttributeForm,
} from "../components/key-value-form/AttributeForm";
import {
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { RealmRoleForm } from "./RealmRoleForm";
import { useRealm } from "../context/realm-context/RealmContext";
import { AddRoleMappingModal } from "../components/role-mapping/AddRoleMappingModal";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { AssociatedRolesTab } from "./AssociatedRolesTab";
import { UsersInRoleTab } from "./UsersInRoleTab";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { toRealmRole } from "./routes/RealmRole";
import {
  ClientRoleParams,
  ClientRoleRoute,
  toClientRole,
} from "./routes/ClientRole";
import { PermissionsTab } from "../components/permission-tab/PermissionTab";

export default function RealmRoleTabs() {
  const { t } = useTranslation("roles");
  const form = useForm<AttributeForm>({
    mode: "onChange",
  });
  const { setValue, getValues, trigger, reset } = form;
  const history = useHistory();

  const { adminClient } = useAdminClient();
  const [role, setRole] = useState<AttributeForm>();

  const { id, clientId } = useParams<{ id: string; clientId: string }>();

  const { url } = useRouteMatch();

  const { realm: realmName } = useRealm();

  const [key, setKey] = useState("");

  const refresh = () => {
    setKey(`${new Date().getTime()}`);
  };

  const { addAlert, addError } = useAlerts();

  const [open, setOpen] = useState(false);
  const convert = (role: RoleRepresentation) => {
    const { attributes, ...rest } = role;
    return {
      attributes: arrayToKeyValue(attributes),
      ...rest,
    };
  };

  const [realm, setRealm] = useState<RealmRepresentation>();

  useFetch(
    async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      if (!id) {
        return { realm };
      }
      const role = await adminClient.roles.findOneById({ id });
      return { realm, role };
    },
    ({ realm, role }) => {
      if (!realm || (!role && id)) {
        throw new Error(t("common:notFound"));
      }

      setRealm(realm);

      if (role) {
        const convertedRole = convert(role);
        setRole(convertedRole);
        Object.entries(convertedRole).map((entry) => {
          setValue(entry[0], entry[1]);
        });
      }
    },
    [key, url]
  );

  const save = async () => {
    try {
      const values = getValues();
      if (
        values.attributes &&
        values.attributes[values.attributes.length - 1]?.key === ""
      ) {
        setValue(
          "attributes",
          values.attributes.slice(0, values.attributes.length - 1)
        );
      }
      if (!(await trigger())) {
        return;
      }
      const { attributes, ...rest } = values;
      let roleRepresentation: RoleRepresentation = rest;

      roleRepresentation.name = roleRepresentation.name?.trim();

      if (id) {
        if (attributes) {
          roleRepresentation.attributes = keyValueToArray(attributes);
        }
        roleRepresentation = {
          ...omit(role!, "attributes"),
          ...roleRepresentation,
        };
        if (!clientId) {
          await adminClient.roles.updateById({ id }, roleRepresentation);
        } else {
          await adminClient.clients.updateRole(
            { id: clientId, roleName: values.name! },
            roleRepresentation
          );
        }

        setRole(convert(roleRepresentation));
      } else {
        let createdRole;
        if (!clientId) {
          await adminClient.roles.create(roleRepresentation);
          createdRole = await adminClient.roles.findOneByName({
            name: values.name!,
          });
        } else {
          await adminClient.clients.createRole({
            id: clientId,
            name: values.name,
          });
          if (values.description) {
            await adminClient.clients.updateRole(
              { id: clientId, roleName: values.name! },
              roleRepresentation
            );
          }
          createdRole = await adminClient.clients.findRole({
            id: clientId,
            roleName: values.name!,
          });
        }
        if (!createdRole) {
          throw new Error(t("common:notFound"));
        }

        setRole(convert(createdRole));
        history.push(
          url.substr(0, url.lastIndexOf("/") + 1) + createdRole.id + "/details"
        );
      }
      addAlert(t(id ? "roleSaveSuccess" : "roleCreated"), AlertVariant.success);
    } catch (error) {
      addError(`roles:${id ? "roleSave" : "roleCreate"}Error`, error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleDeleteConfirm",
    messageKey: t("roles:roleDeleteConfirmDialog", {
      name: role?.name || t("createRole"),
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        if (!clientId) {
          await adminClient.roles.delById({ id });
        } else {
          await adminClient.clients.delRole({
            id: clientId,
            roleName: role!.name as string,
          });
        }
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
        history.push(url.substr(0, url.indexOf("/roles") + "/roles".length));
      } catch (error) {
        addError("roles:roleDeleteError", error);
      }
    },
  });

  const dropdownItems = url.includes("associated-roles")
    ? [
        <DropdownItem
          key="delete-all-associated"
          component="button"
          onClick={() => toggleDeleteAllAssociatedRolesDialog()}
        >
          {t("roles:removeAllAssociatedRoles")}
        </DropdownItem>,
        <DropdownItem
          key="delete-role"
          component="button"
          onClick={() => {
            toggleDeleteDialog();
          }}
        >
          {t("deleteRole")}
        </DropdownItem>,
      ]
    : [
        <DropdownItem
          key="toggle-modal"
          data-testid="add-roles"
          component="button"
          onClick={() => toggleModal()}
        >
          {t("addAssociatedRolesText")}
        </DropdownItem>,
        <DropdownItem
          key="delete-role"
          component="button"
          onClick={() => toggleDeleteDialog()}
        >
          {t("deleteRole")}
        </DropdownItem>,
      ];

  const [
    toggleDeleteAllAssociatedRolesDialog,
    DeleteAllAssociatedRolesConfirm,
  ] = useConfirmDialog({
    titleKey: t("roles:removeAllAssociatedRoles") + "?",
    messageKey: t("roles:removeAllAssociatedRolesConfirmDialog", {
      name: role?.name || t("createRole"),
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        const additionalRoles = await adminClient.roles.getCompositeRoles({
          id: role!.id!,
        });
        await adminClient.roles.delCompositeRoles({ id }, additionalRoles);
        addAlert(
          t("compositeRoleOff"),
          AlertVariant.success,
          t("compositesRemovedAlertDescription")
        );
        const loc = url.replace(/\/AssociatedRoles/g, "/details");
        history.push(loc);
        refresh();
      } catch (error) {
        addError("roles:roleDeleteError", error);
      }
    },
  });

  const toggleModal = () => {
    setOpen(!open);
  };

  const clientRoleRouteMatch = useRouteMatch<ClientRoleParams>(
    ClientRoleRoute.path
  );

  const toAssociatedRoles = () => {
    const to = clientRoleRouteMatch
      ? toClientRole({
          ...clientRoleRouteMatch.params,
          tab: "associated-roles",
        })
      : toRealmRole({
          realm: realm?.realm!,
          id,
          tab: "associated-roles",
        });
    history.push(to);
  };

  const addComposites = async (composites: RoleRepresentation[]) => {
    try {
      await adminClient.roles.createComposite(
        { roleId: role?.id!, realm: realm!.realm },
        composites
      );
      refresh();
      toAssociatedRoles();
      addAlert(t("addAssociatedRolesSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roles:addAssociatedRolesError", error);
    }
  };

  const isDefaultRole = (name: string) => realm?.defaultRole!.name === name;

  if (!realm) {
    return <KeycloakSpinner />;
  }
  if (!role) {
    return (
      <RealmRoleForm
        reset={() => reset(role)}
        form={form}
        save={save}
        editMode={false}
      />
    );
  }

  return (
    <>
      <DeleteConfirm />
      <DeleteAllAssociatedRolesConfirm />
      {open && (
        <AddRoleMappingModal
          id={id}
          type="roles"
          name={role.name}
          onAssign={(rows) => addComposites(rows.map((r) => r.role))}
          onClose={() => setOpen(false)}
        />
      )}
      <ViewHeader
        titleKey={role.name || t("createRole")}
        badges={[
          {
            id: "composite-role-badge",
            text: role.composite ? t("composite") : "",
            readonly: true,
          },
        ]}
        subKey={id ? "" : "roles:roleCreateExplain"}
        actionsDropdownId="roles-actions-dropdown"
        dropdownItems={dropdownItems}
        divider={!id}
      />
      <PageSection variant="light" className="pf-u-p-0">
        {id && (
          <KeycloakTabs isBox mountOnEnter>
            <Tab
              eventKey="details"
              title={<TabTitleText>{t("common:details")}</TabTitleText>}
            >
              <RealmRoleForm
                reset={() => reset(role)}
                form={form}
                save={save}
                editMode={true}
              />
            </Tab>
            {role.composite && (
              <Tab
                eventKey="associated-roles"
                className="kc-associated-roles-tab"
                title={<TabTitleText>{t("associatedRolesText")}</TabTitleText>}
              >
                <AssociatedRolesTab parentRole={role} refresh={refresh} />
              </Tab>
            )}
            {!isDefaultRole(role.name!) && (
              <Tab
                eventKey="attributes"
                className="kc-attributes-tab"
                title={<TabTitleText>{t("common:attributes")}</TabTitleText>}
              >
                <AttributesForm
                  form={form}
                  save={save}
                  reset={() => reset(role)}
                />
              </Tab>
            )}
            {!isDefaultRole(role.name!) && (
              <Tab
                eventKey="users-in-role"
                title={<TabTitleText>{t("usersInRole")}</TabTitleText>}
              >
                <UsersInRoleTab data-cy="users-in-role-tab" />
              </Tab>
            )}
            <Tab
              eventKey="permissions"
              title={<TabTitleText>{t("common:permissions")}</TabTitleText>}
            >
              <PermissionsTab id={role.id} type="roles" />
            </Tab>
          </KeycloakTabs>
        )}
      </PageSection>
    </>
  );
}
