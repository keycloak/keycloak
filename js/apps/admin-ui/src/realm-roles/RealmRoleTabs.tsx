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
import { useState } from "react";
import { SubmitHandler, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useLocation, useMatch, useNavigate } from "react-router-dom";

import { toClient } from "../clients/routes/Client";
import {
  ClientRoleParams,
  ClientRoleRoute,
  ClientRoleTab,
  toClientRole,
} from "../clients/routes/ClientRole";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form/AttributeForm";
import {
  arrayToKeyValue,
  keyValueToArray,
  KeyValueType,
} from "../components/key-value-form/key-value-convert";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { PermissionsTab } from "../components/permission-tab/PermissionTab";
import { RoleForm } from "../components/role-form/RoleForm";
import { AddRoleMappingModal } from "../components/role-mapping/AddRoleMappingModal";
import { RoleMapping } from "../components/role-mapping/RoleMapping";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useParams } from "../utils/useParams";
import { RealmRoleRoute, RealmRoleTab, toRealmRole } from "./routes/RealmRole";
import { toRealmRoles } from "./routes/RealmRoles";
import { UsersInRoleTab } from "./UsersInRoleTab";

export default function RealmRoleTabs() {
  const { t } = useTranslation("roles");
  const form = useForm<AttributeForm>({
    mode: "onChange",
  });
  const { control, reset, setValue } = form;
  const navigate = useNavigate();

  const { adminClient } = useAdminClient();

  const { id, clientId } = useParams<ClientRoleParams>();
  const { pathname } = useLocation();

  const { realm: realmName } = useRealm();

  const [key, setKey] = useState(0);
  const [attributes, setAttributes] = useState<KeyValueType[] | undefined>();

  const { profileInfo } = useServerInfo();

  const refresh = () => setKey(key + 1);

  const { addAlert, addError } = useAlerts();

  const [open, setOpen] = useState(false);
  const convert = (role: RoleRepresentation) => {
    const { attributes, ...rest } = role;
    return {
      attributes: arrayToKeyValue(attributes),
      ...rest,
    };
  };

  const roleName = useWatch({
    control,
    defaultValue: undefined,
    name: "name",
  });

  const composites = useWatch({
    control,
    defaultValue: false,
    name: "composite",
  });

  const [realm, setRealm] = useState<RealmRepresentation>();

  useFetch(
    async () => {
      const [realm, role] = await Promise.all([
        adminClient.realms.findOne({ realm: realmName }),
        adminClient.roles.findOneById({ id }),
      ]);

      return { realm, role };
    },
    ({ realm, role }) => {
      if (!realm || !role) {
        throw new Error(t("common:notFound"));
      }

      const convertedRole = convert(role);

      reset(convertedRole);
      setAttributes(convertedRole.attributes);
      setRealm(realm);
    },
    [key]
  );

  const onSubmit: SubmitHandler<AttributeForm> = async (formValues) => {
    try {
      const { attributes, ...rest } = formValues;
      const roleRepresentation: RoleRepresentation = rest;

      roleRepresentation.name = roleRepresentation.name?.trim();
      roleRepresentation.attributes = keyValueToArray(attributes);

      if (!clientId) {
        await adminClient.roles.updateById({ id }, roleRepresentation);
      } else {
        await adminClient.clients.updateRole(
          { id: clientId, roleName: formValues.name! },
          roleRepresentation
        );
      }

      setAttributes(attributes);
      addAlert(t("roleSaveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roles:roleSaveError", error);
    }
  };

  const realmRoleMatch = useMatch(RealmRoleRoute.path);
  const clientRoleMatch = useMatch(ClientRoleRoute.path);

  const toOverview = () => {
    if (realmRoleMatch) {
      return toRealmRoles({ realm: realmName });
    }

    if (clientRoleMatch) {
      return toClient({
        realm: realmName,
        clientId: clientRoleMatch.params.clientId!,
        tab: "roles",
      });
    }

    throw new Error("Roles overview route could not be determined.");
  };

  const toTab = (tab: RealmRoleTab | ClientRoleTab) => {
    if (realmRoleMatch) {
      return toRealmRole({
        realm: realmName,
        id,
        tab,
      });
    }

    if (clientRoleMatch) {
      return toClientRole({
        realm: realmName,
        id,
        clientId: clientRoleMatch.params.clientId!,
        tab: tab as ClientRoleTab,
      });
    }

    throw new Error("Route could not be determined.");
  };

  const useTab = (tab: RealmRoleTab | ClientRoleTab) =>
    useRoutableTab(toTab(tab));

  const detailsTab = useTab("details");
  const associatedRolesTab = useTab("associated-roles");
  const attributesTab = useTab("attributes");
  const usersInRoleTab = useTab("users-in-role");
  const permissionsTab = useTab("permissions");

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleDeleteConfirm",
    messageKey: t("roles:roleDeleteConfirmDialog", {
      selectedRoleName: roleName || t("createRole"),
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
            roleName: roleName!,
          });
        }
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
        navigate(toOverview());
      } catch (error) {
        addError("roles:roleDeleteError", error);
      }
    },
  });

  const dropdownItems = pathname.includes("associated-roles")
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
      name: roleName || t("createRole"),
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        const additionalRoles = await adminClient.roles.getCompositeRoles({
          id,
        });
        await adminClient.roles.delCompositeRoles({ id }, additionalRoles);
        addAlert(
          t("compositeRoleOff"),
          AlertVariant.success,
          t("compositesRemovedAlertDescription")
        );
        navigate(toTab("details"));
        refresh();
      } catch (error) {
        addError("roles:roleDeleteError", error);
      }
    },
  });

  const toggleModal = () => {
    setOpen(!open);
  };

  const addComposites = async (composites: RoleRepresentation[]) => {
    try {
      await adminClient.roles.createComposite(
        { roleId: id, realm: realm!.realm },
        composites
      );
      refresh();
      navigate(toTab("associated-roles"));
      addAlert(t("addAssociatedRolesSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roles:addAssociatedRolesError", error);
    }
  };

  const isDefaultRole = (name: string | undefined) =>
    realm?.defaultRole!.name === name;

  if (!realm) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <DeleteConfirm />
      <DeleteAllAssociatedRolesConfirm />
      {open && (
        <AddRoleMappingModal
          id={id}
          type="roles"
          name={roleName}
          onAssign={(rows) => addComposites(rows.map((r) => r.role))}
          onClose={() => setOpen(false)}
        />
      )}
      <ViewHeader
        titleKey={roleName!}
        badges={[
          {
            id: "composite-role-badge",
            text: composites ? t("composite") : "",
            readonly: true,
          },
        ]}
        actionsDropdownId="roles-actions-dropdown"
        dropdownItems={dropdownItems}
        divider={false}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs isBox mountOnEnter defaultLocation={toTab("details")}>
          <Tab
            title={<TabTitleText>{t("common:details")}</TabTitleText>}
            {...detailsTab}
          >
            <RoleForm
              form={form}
              onSubmit={onSubmit}
              role={clientRoleMatch ? "manage-clients" : "manage-realm"}
              cancelLink={
                clientRoleMatch
                  ? toClient({ realm: realmName, clientId, tab: "roles" })
                  : toRealmRoles({ realm: realmName })
              }
              editMode
            />
          </Tab>
          {composites && (
            <Tab
              data-testid="associatedRolesTab"
              title={<TabTitleText>{t("associatedRolesText")}</TabTitleText>}
              {...associatedRolesTab}
            >
              <RoleMapping
                name={roleName!}
                id={id}
                type="roles"
                isManager
                save={(rows) => addComposites(rows.map((r) => r.role))}
              />
            </Tab>
          )}
          {!isDefaultRole(roleName) && (
            <Tab
              data-testid="attributesTab"
              className="kc-attributes-tab"
              title={<TabTitleText>{t("common:attributes")}</TabTitleText>}
              {...attributesTab}
            >
              <AttributesForm
                form={form}
                save={onSubmit}
                reset={() =>
                  setValue("attributes", attributes, { shouldDirty: false })
                }
              />
            </Tab>
          )}
          {!isDefaultRole(roleName) && (
            <Tab
              title={<TabTitleText>{t("usersInRole")}</TabTitleText>}
              {...usersInRoleTab}
            >
              <UsersInRoleTab data-cy="users-in-role-tab" />
            </Tab>
          )}
          {!profileInfo?.disabledFeatures?.includes(
            "ADMIN_FINE_GRAINED_AUTHZ"
          ) && (
            <Tab
              title={<TabTitleText>{t("common:permissions")}</TabTitleText>}
              {...permissionsTab}
            >
              <PermissionsTab id={id} type="roles" />
            </Tab>
          )}
        </RoutableTabs>
      </PageSection>
    </>
  );
}
