import type OrganizationRoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRoleRepresentation";
import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  ButtonVariant,
  DropdownItem,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useState } from "react";
import {
  FormProvider,
  SubmitHandler,
  useForm,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form/AttributeForm";
import {
  KeyValueType,
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";
import { PermissionsTab } from "../components/permission-tab/PermissionTab";
import { RoleForm } from "../components/role-form/RoleForm";
import { RoleMapping } from "../components/role-mapping/RoleMapping";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { AdminEvents } from "../events/AdminEvents";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { toEditOrganization } from "./routes/EditOrganization";
import type {
  OrganizationRoleParams,
  OrganizationRoleTab,
} from "./routes/OrganizationRole";
import { OrganizationRoleMembersTab } from "./OrganizationRoleMembersTab";

type OrganizationRoleForm = Omit<OrganizationRoleRepresentation, "attributes"> &
  AttributeForm;

export default function OrganizationRoleTabs() {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { orgId, roleId, tab } = useParams<OrganizationRoleParams>();

  const { adminClient } = useAdminClient();
  const isFeatureEnabled = useIsFeatureEnabled();

  const [attributes, setAttributes] = useState<KeyValueType[]>([]);
  const [key, setKey] = useState(0);

  const form = useForm<OrganizationRoleForm>({ mode: "onChange" });
  const { reset, control, setValue } = form;

  const isManager = hasAccess("manage-users");
  const canViewPermissionsTab = hasAccess(
    "query-clients", 
    "manage-authorization"
  );

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

  const refresh = () => setKey(key + 1);

  useFetch(
    async () => {
      const role = await adminClient.organizations.findRole({
        orgId: orgId!,
        roleId: roleId!,
      });
      return role;
    },
    (role) => {
      if (!role) {
        throw new Error(t("notFound"));
      }

      const convertedRole = {
        ...role,
        attributes: arrayToKeyValue(role.attributes),
      };

      reset(convertedRole);
      setAttributes(convertedRole.attributes || []);
    },
    [key],
  );

  const onSubmit: SubmitHandler<OrganizationRoleForm> = async (formValues) => {
    try {
      const { attributes, ...rest } = formValues;
      const roleRepresentation: OrganizationRoleRepresentation = {
        ...rest,
        attributes: keyValueToArray(attributes),
      };

      roleRepresentation.name = roleRepresentation.name?.trim();

      await adminClient.organizations.updateRole(
        { orgId: orgId!, roleId: roleId! },
        roleRepresentation,
      );

      setAttributes(attributes || []);
      addAlert(t("organizationRoleSaveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("organizationRoleSaveError", error);
    }
  };

  const addComposites = async (composites: OrganizationRoleRepresentation[]) => {
    try {
      // Organization roles composite support - implement if backend supports it
      refresh();
      navigate(toTab("associated-roles"));
      addAlert(t("addAssociatedRolesSuccess"), AlertVariant.success);
    } catch (error) {
      addError("addAssociatedRolesError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteRole",
    messageKey: "deleteRoleConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.organizations.delRole({
          orgId: orgId!,
          roleId: roleId!,
        });
        addAlert(t("organizationRoleDeleteSuccess"), AlertVariant.success);
        navigate(toEditOrganization({ realm, id: orgId!, tab: "roles" }));
      } catch (error) {
        addError("organizationRoleDeleteError", error);
      }
    },
  });

  const toTab = (tab: OrganizationRoleTab) => ({
    pathname: `/${realm}/organizations/${orgId}/roles/${roleId}/${tab}`,
  });

  const detailsTab = useRoutableTab(toTab("details"));
  const associatedRolesTab = useRoutableTab(toTab("associated-roles"));
  const attributesTab = useRoutableTab(toTab("attributes"));
  const membersTab = useRoutableTab(toTab("members"));
  const permissionsTab = useRoutableTab(toTab("permissions"));
  const eventsTab = useRoutableTab(toTab("events"));

  if (!orgId || !roleId) return <KeycloakSpinner />;

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={roleName!}
        badges={[
          {
            id: "composite-role-badge",
            text: composites ? t("composite") : "",
            readonly: true,
          },
        ]}
        actionsDropdownId="organization-roles-actions-dropdown"
        dropdownItems={
          isManager
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-role"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
        divider={false}
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <FormProvider {...form}>
          <RoutableTabs 
            isBox 
            mountOnEnter={false} 
            defaultLocation={toTab("details")}
          >
            <Tab
              title={<TabTitleText>{t("details")}</TabTitleText>}
              data-testid="details"
              {...detailsTab}
            >
              <RoleForm
                form={form}
                onSubmit={onSubmit}
                cancelLink={toEditOrganization({ realm, id: orgId!, tab: "roles" })}
                role="manage-realm"
                editMode={true}
              />
            </Tab>
            <Tab
              data-testid="associatedRolesTab"
              title={<TabTitleText>{t("associatedRolesText")}</TabTitleText>}
              {...associatedRolesTab}
            >
              <RoleMapping
                name={roleName!}
                id={roleId!}
                type="roles"
                isManager
                save={(rows) => addComposites(rows.map((r) => r.role))}
              />
            </Tab>
            <Tab
              data-testid="attributesTab"
              className="kc-attributes-tab"
              title={<TabTitleText>{t("attributes")}</TabTitleText>}
              {...attributesTab}
            >
              <AttributesForm
                form={form}
                save={onSubmit}
                fineGrainedAccess={false}
                reset={() =>
                  setValue("attributes", attributes, { shouldDirty: false })
                }
              />
            </Tab>
            <Tab
              data-testid="membersTab"
              title={<TabTitleText>{t("members")}</TabTitleText>}
              {...membersTab}
            >
              <OrganizationRoleMembersTab />
            </Tab>
            {isFeatureEnabled(Feature.AdminFineGrainedAuthz) &&
              canViewPermissionsTab && (
                <Tab
                  title={<TabTitleText>{t("permissions")}</TabTitleText>}
                  {...permissionsTab}
                >
                  <PermissionsTab id={roleId!} type="roles" />
                </Tab>
              )}
            {hasAccess("view-events") && (
              <Tab
                data-testid="admin-events-tab"
                title={<TabTitleText>{t("adminEvents")}</TabTitleText>}
                {...eventsTab}
              >
                <AdminEvents resourcePath={`organizations/${orgId}/roles/${roleId}`} />
              </Tab>
            )}
          </RoutableTabs>
        </FormProvider>
      </PageSection>
    </>
  );
}
