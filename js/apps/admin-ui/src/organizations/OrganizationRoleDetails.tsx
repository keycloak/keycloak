import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
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
import { useNavigate } from "react-router-dom";
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
import { RoleForm } from "../components/role-form/RoleForm";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { AdminEvents } from "../events/AdminEvents";
import { useParams } from "../utils/useParams";
import { OrganizationRoleComposites } from "./OrganizationRoleComposites";
import { OrganizationRoleUsers } from "./OrganizationRoleUsers";
import { toEditOrganization } from "./routes/EditOrganization";
import {
  OrganizationRoleParams,
  OrganizationRoleTab,
  toOrganizationRole,
} from "./routes/OrganizationRole";

type OrganizationRoleForm = Omit<RoleRepresentation, "attributes"> &
  AttributeForm;

export default function OrganizationRoleDetails() {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const { realm } = useRealm();
  const { orgId, roleId: routeRoleId } = useParams<OrganizationRoleParams>();
  const navigate = useNavigate();
  const form = useForm<OrganizationRoleForm>({ mode: "onChange" });
  const { control, reset, setValue } = form;
  const [role, setRole] = useState<RoleRepresentation>();
  const [attributes, setAttributes] = useState<KeyValueType[]>([]);
  const [key, setKey] = useState(0);
  const isDefault = routeRoleId === "default";
  const legacyCanManageOrganizations = hasAccess("manage-organizations");
  const roleName = useWatch({ control, name: "name" });
  const isComposite = useWatch({ control, name: "composite" });

  useFetch(
    () =>
      isDefault
        ? adminClient.organizations.findDefaultRole({ orgId })
        : adminClient.organizations.findRole({ orgId, roleId: routeRoleId }),
    (loadedRole) => {
      if (!loadedRole) {
        throw new Error(t("notFound"));
      }
      const converted = {
        ...loadedRole,
        attributes: arrayToKeyValue(loadedRole.attributes),
      };
      setRole(loadedRole);
      setAttributes(converted.attributes);
      reset(converted);
    },
    [orgId, routeRoleId, key],
  );

  const toTab = (tab: OrganizationRoleTab) =>
    toOrganizationRole({
      realm,
      orgId,
      roleId: routeRoleId,
      tab,
    });

  const detailsTab = useRoutableTab(toTab("details"));
  const associatedRolesTab = useRoutableTab(toTab("associated-roles"));
  const attributesTab = useRoutableTab(toTab("attributes"));
  const usersTab = useRoutableTab(toTab("users-in-role"));
  const eventsTab = useRoutableTab(toTab("events"));

  const onSubmit: SubmitHandler<OrganizationRoleForm> = async (values) => {
    try {
      const { attributes: formAttributes, ...rest } = values;
      await adminClient.organizations.updateRole(
        { orgId, roleId: role!.id! },
        {
          ...rest,
          name: rest.name?.trim(),
          attributes: keyValueToArray(formAttributes),
        },
      );
      setAttributes(formAttributes ?? []);
      addAlert(t("organizationRoleSaved"), AlertVariant.success);
      setKey((value) => value + 1);
    } catch (error) {
      addError("organizationRoleSaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roleDeleteConfirm",
    messageKey: t("roleDeleteConfirmDialog", {
      selectedRoleName: roleName ?? "",
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.organizations.delRole({
          orgId,
          roleId: role!.id!,
        });
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
        navigate(toEditOrganization({ realm, id: orgId, tab: "roles" }));
      } catch (error) {
        addError("roleDeleteError", error);
      }
    },
  });

  if (!role?.id) {
    return <KeycloakSpinner />;
  }

  const canManageRole = role.access?.manage ?? legacyCanManageOrganizations;
  const canMapRole = role.access?.mapRole ?? canManageRole;

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={roleName ?? ""}
        badges={[
          {
            id: "organization-role-composite-badge",
            text: isComposite ? t("composite") : "",
            readonly: true,
          },
          {
            id: "default-organization-role-badge",
            text: isDefault ? t("default") : "",
            readonly: true,
          },
        ]}
        actionsDropdownId="organization-role-actions-dropdown"
        dropdownItems={
          canManageRole && !isDefault
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-organization-role"
                  onClick={toggleDeleteDialog}
                >
                  {t("deleteRole")}
                </DropdownItem>,
              ]
            : undefined
        }
        divider={false}
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <FormProvider {...form}>
          <RoutableTabs isBox mountOnEnter defaultLocation={toTab("details")}>
            <Tab
              data-testid="organization-role-details-tab"
              title={<TabTitleText>{t("details")}</TabTitleText>}
              {...detailsTab}
            >
              <RoleForm
                form={form}
                onSubmit={onSubmit}
                cancelLink={toEditOrganization({
                  realm,
                  id: orgId,
                  tab: "roles",
                })}
                role="manage-organizations"
                editMode
                isReadOnly={!canManageRole}
              />
            </Tab>
            <Tab
              data-testid="organization-role-associated-roles-tab"
              title={<TabTitleText>{t("associatedRolesText")}</TabTitleText>}
              {...associatedRolesTab}
            >
              <OrganizationRoleComposites
                organizationId={orgId}
                roleId={role.id}
                roleName={roleName ?? ""}
                canManage={canManageRole}
              />
            </Tab>
            {!isDefault && (
              <Tab
                data-testid="organization-role-attributes-tab"
                title={<TabTitleText>{t("attributes")}</TabTitleText>}
                {...attributesTab}
              >
                <AttributesForm
                  form={form}
                  save={onSubmit}
                  fineGrainedAccess={canManageRole}
                  reset={() =>
                    setValue("attributes", attributes, { shouldDirty: false })
                  }
                />
              </Tab>
            )}
            {!isDefault && (
              <Tab
                data-testid="organization-role-users-tab"
                title={<TabTitleText>{t("usersInRole")}</TabTitleText>}
                {...usersTab}
              >
                <OrganizationRoleUsers
                  organizationId={orgId}
                  roleId={role.id}
                  canMapRole={canMapRole}
                />
              </Tab>
            )}
            {hasAccess("view-events") && (
              <Tab
                data-testid="organization-role-events-tab"
                title={<TabTitleText>{t("adminEvents")}</TabTitleText>}
                {...eventsTab}
              >
                <AdminEvents
                  resourcePath={`organizations/${orgId}/roles/${role.id}`}
                />
              </Tab>
            )}
          </RoutableTabs>
        </FormProvider>
      </PageSection>
    </>
  );
}
