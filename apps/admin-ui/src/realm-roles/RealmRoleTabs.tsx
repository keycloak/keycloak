import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { omit } from "lodash-es";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useRouteMatch } from "react-router-dom";
import { useNavigate } from "react-router-dom-v5-compat";

import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form/AttributeForm";
import {
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { PermissionsTab } from "../components/permission-tab/PermissionTab";
import { AddRoleMappingModal } from "../components/role-mapping/AddRoleMappingModal";
import { RoleMapping } from "../components/role-mapping/RoleMapping";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useParams } from "../utils/useParams";
import { RealmRoleForm } from "./RealmRoleForm";
import {
  ClientRoleParams,
  ClientRoleRoute,
  toClientRole,
} from "./routes/ClientRole";
import { toRealmRole } from "./routes/RealmRole";
import { UsersInRoleTab } from "./UsersInRoleTab";

export default function RealmRoleTabs() {
  const { t } = useTranslation("roles");
  const form = useForm<AttributeForm>({
    mode: "onChange",
  });
  const { setValue, getValues, trigger, reset } = form;
  const navigate = useNavigate();

  const { adminClient } = useAdminClient();
  const [role, setRole] = useState<AttributeForm>();

  const { id, clientId } = useParams<{ id: string; clientId: string }>();

  const { url } = useRouteMatch();

  const { realm: realmName } = useRealm();

  const [key, setKey] = useState(0);

  const { profileInfo } = useServerInfo();

  const refresh = () => {
    setKey(key + 1);
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
        navigate(
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
      selectedRoleName: role?.name || t("createRole"),
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
        navigate(url.substr(0, url.indexOf("/roles") + "/roles".length));
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
        toDetail();
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

  const toDetail = () => {
    const to = clientRoleRouteMatch
      ? toClientRole({
          ...clientRoleRouteMatch.params,
          tab: "details",
        })
      : toRealmRole({
          realm: realm?.realm!,
          id,
          tab: "details",
        });
    navigate(to);
  };

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
    navigate(to);
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
                <RoleMapping
                  name={role.name!}
                  id={role.id!}
                  type="roles"
                  isManager
                  save={(rows) => addComposites(rows.map((r) => r.role))}
                />
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
            {!profileInfo?.disabledFeatures?.includes(
              "ADMIN_FINE_GRAINED_AUTHZ"
            ) && (
              <Tab
                eventKey="permissions"
                title={<TabTitleText>{t("common:permissions")}</TabTitleText>}
              >
                <PermissionsTab id={role.id} type="roles" />
              </Tab>
            )}
          </KeycloakTabs>
        )}
      </PageSection>
    </>
  );
}
