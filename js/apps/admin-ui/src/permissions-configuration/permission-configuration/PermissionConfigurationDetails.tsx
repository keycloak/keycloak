import ResourceServerRepresentation, {
  ResourceTypesRepresentation,
} from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  PageSection,
} from "@patternfly/react-core";
import { useMemo, useState, type JSX } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useParams } from "../../utils/useParams";
import {
  PermissionConfigurationDetailsParams,
  toPermissionConfigurationDetails,
} from "../routes/PermissionConfigurationDetails";
import { toPermissionsConfigurationTabs } from "../routes/PermissionsConfigurationTabs";
import PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import { AssignedPolicies } from "./AssignedPolicies";
import { ScopePicker } from "../../clients/authorization/ScopePicker";
import { Users } from "./permission-type/Users";
import { sortBy } from "lodash-es";
import { NameDescription } from "../../clients/authorization/policy/NameDescription";

const COMPONENTS: {
  [index: string]: () => JSX.Element;
} = {
  Users: Users,
} as const;

export const isValidComponentType = (value: string) => value in COMPONENTS;

export default function PermissionConfigurationDetails() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm, permissionClientId, permissionId, resourceType } =
    useParams<PermissionConfigurationDetailsParams>();
  const navigate = useNavigate();
  const form = useForm();
  const { handleSubmit } = form;
  const { addAlert, addError } = useAlerts();
  const [permission, setPermission] = useState<PolicyRepresentation>();
  const [providers, setProviders] = useState<PolicyProviderRepresentation[]>();
  const [policies, setPolicies] = useState<PolicyRepresentation[]>();
  const [adminPermissionClient, setAdminPermissionClient] =
    useState<ResourceServerRepresentation>();
  const resourceTypeScopes = useMemo(() => {
    const resourceTypes =
      adminPermissionClient?.authorizationSchema?.resourceTypes ?? {};
    return (Object.values(resourceTypes) as ResourceTypesRepresentation[])
      .filter((resource) => resource.type === resourceType)
      .flatMap((resource) => resource.scopes || [])
      .map((scope) => scope || "");
  }, [adminPermissionClient, resourceType]);

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.getResourceServer({
          id: permissionClientId,
        }),
        adminClient.clients.listPolicyProviders({
          id: permissionClientId,
        }),
        adminClient.clients.listPolicies({
          id: permissionClientId,
          permission: "false",
        }),
      ]),
    ([adminClient, providers, policies]) => {
      const filteredProviders = providers.filter(
        (p) => p.type !== "resource" && p.type !== "scope",
      );
      setAdminPermissionClient(adminClient);
      setProviders(
        sortBy(
          filteredProviders,
          (provider: PolicyProviderRepresentation) => provider.type,
        ),
      );
      setPolicies(policies || []);
    },
    [permissionClientId],
  );

  const save = async (permission: PolicyRepresentation) => {
    try {
      const newPermission = {
        ...permission,
        policies: permission.policies?.map((policy: any) => policy.id),
        scopes: permission.scopes?.map((scope: any) => scope.name),
        resourceType: resourceType,
      };

      if (permissionId) {
        await adminClient.clients.updatePermission(
          { id: permissionClientId, type: "scope", permissionId },
          newPermission,
        );
      } else {
        const result = await adminClient.clients.createPermission(
          { id: permissionClientId, type: "scope" },
          newPermission,
        );
        setPermission(result);
        navigate(
          toPermissionConfigurationDetails({
            realm,
            permissionClientId: permissionClientId,
            permissionId: result.id!,
            resourceType,
          }),
        );
      }

      addAlert(
        t(permissionId ? "updatePermissionSuccess" : "createPermissionSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("permissionSaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deletePermission",
    messageKey: t("deletePermissionConfirm", {
      permission: permission?.name,
    }),
    continueButtonVariant: ButtonVariant.danger,
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delPermission({
          id: permissionClientId!,
          type: "scope",
          permissionId: permissionId,
        });
        addAlert(t("permissionDeletedSuccess"), AlertVariant.success);
        navigate(toPermissionsConfigurationTabs({ realm, tab: "permissions" }));
      } catch (error) {
        addError("permissionDeletedError", error);
      }
    },
  });

  if (permissionId && !permission) {
    return <KeycloakSpinner />;
  }

  function getComponentType() {
    return isValidComponentType(resourceType)
      ? COMPONENTS[resourceType]
      : COMPONENTS["js"];
  }

  const ComponentType = getComponentType();

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={
          permissionId
            ? permission?.name!
            : t("createPermissionOfType", { resourceType })
        }
        subKey={
          permissionId
            ? permission?.description
            : t(`resourceType.${resourceType}`)
        }
        dropdownItems={
          permissionId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-permission"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess isHorizontal onSubmit={handleSubmit(save)} role="anyone">
          <FormProvider {...form}>
            <NameDescription clientId={permissionClientId} />
            <ScopePicker
              clientId={permissionClientId}
              resourceTypeScopes={resourceTypeScopes ?? []}
            />
            <ComponentType />
            <AssignedPolicies
              permissionClientId={permissionClientId}
              providers={providers!}
              policies={policies!}
              resourceType={resourceType}
            />
          </FormProvider>
          <ActionGroup>
            <div className="pf-v5-u-mt-md">
              <Button
                variant={ButtonVariant.primary}
                className="pf-v5-u-mr-md"
                type="submit"
                data-testid="save"
              >
                {t("save")}
              </Button>
              <Button
                variant="link"
                data-testid="cancel"
                component={(props) => (
                  <Link
                    {...props}
                    to={toPermissionsConfigurationTabs({
                      realm,
                      tab: "permissions",
                    })}
                  />
                )}
              >
                {t("cancel")}
              </Button>
            </div>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
