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
import { useMemo, useState } from "react";
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
import { ResourceType } from "../resource-types/ResourceType";
import { sortBy } from "lodash-es";
import { NameDescription } from "../../clients/authorization/policy/NameDescription";
import useSortedResourceTypes from "../../utils/useSortedResourceTypes";

export default function PermissionConfigurationDetails() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm, permissionClientId, permissionId, resourceType } =
    useParams<PermissionConfigurationDetailsParams>();
  const navigate = useNavigate();
  const form = useForm();
  const { handleSubmit, reset } = form;
  const { addAlert, addError } = useAlerts();
  const [permission, setPermission] = useState<PolicyRepresentation>();
  const [providers, setProviders] = useState<PolicyProviderRepresentation[]>();
  const [policies, setPolicies] = useState<PolicyRepresentation[]>();
  const resourceTypes = useSortedResourceTypes({
    clientId: permissionClientId,
  });

  const resourceTypeScopes = useMemo(
    () =>
      resourceTypes
        .filter(({ type }) => type === resourceType)
        .flatMap(({ scopes = [] }) => scopes)
        .map((scope) => scope || ""),
    [resourceTypes, resourceType],
  );

  useFetch(
    async () => {
      if (!permissionClientId) {
        return {};
      }

      const [providers, policies] = await Promise.all([
        adminClient.clients.listPolicyProviders({ id: permissionClientId }),
        adminClient.clients.listPolicies({
          id: permissionClientId,
          permission: "false",
        }),
      ]);

      return { providers, policies };
    },
    ({ providers, policies }) => {
      const filteredProviders = providers?.filter(
        (p) => p.type !== "resource" && p.type !== "scope",
      );

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

  useFetch(
    async () => {
      if (!permissionId) {
        return {};
      }
      const [permission, resources, policies, scopes] = await Promise.all([
        adminClient.clients.findOnePermission({
          id: permissionClientId,
          type: "scope",
          permissionId,
        }),
        adminClient.clients.getAssociatedResources({
          id: permissionClientId,
          permissionId,
        }),
        adminClient.clients.getAssociatedPolicies({
          id: permissionClientId,
          permissionId,
        }),
        adminClient.clients.getAssociatedScopes({
          id: permissionClientId,
          permissionId,
        }),
      ]);

      if (!permission) {
        throw new Error(t("notFound"));
      }

      return {
        permission,
        resources,
        policies,
        scopes,
      };
    },
    ({ permission, resources, policies, scopes }) => {
      const resourceIds = resources?.map((resource) => resource.name!) || [];
      const policyIds = policies?.map((policy) => policy.id!) || [];
      const scopeNames = scopes?.map((scope) => scope.name) || [];

      reset({
        ...permission,
        resources: resourceIds!,
        policies,
        scopes,
      });

      setPermission({
        ...permission,
        resources: resourceIds!,
        policies: policyIds,
        scopes: scopeNames,
      });
    },
    [permissionClientId, permissionId],
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
    messageKey: t("deleteAdminPermissionConfirm", {
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
        navigate(
          toPermissionsConfigurationTabs({
            realm,
            permissionClientId,
            tab: "permissions",
          }),
        );
      } catch (error) {
        addError("permissionDeletedError", error);
      }
    },
  });

  if (!permission) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={permissionId ? permission?.name! : t("createPermission")}
        subKey={
          permissionId
            ? permission?.description!
            : t("createPermissionOfType", { resourceType })
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
            <ResourceType resourceType={resourceType} />
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
                      permissionClientId,
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
