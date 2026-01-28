import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import {
  KeycloakSpinner,
  ListEmptyState,
  PaginatingTableToolbar,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  ExpandableRowContent,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import useSortedResourceTypes from "../../utils/useSortedResourceTypes";
import useToggle from "../../utils/useToggle";
import { AuthorizationScopesDetails } from "../permission-configuration/AuthorizationScopesDetails";
import { SearchDropdown, SearchForm } from "../resource-types/SearchDropdown";
import { toCreatePermissionConfiguration } from "../routes/NewPermissionConfiguration";
import { toPermissionConfigurationDetails } from "../routes/PermissionConfigurationDetails";
import { NewPermissionConfigurationDialog } from "./NewPermissionConfigurationDialog";

type PermissionsConfigurationProps = {
  clientId: string;
};

type ExpandablePolicyRepresentation = PolicyRepresentation & {
  policies?: PolicyRepresentation[];
  resources?: ResourceRepresentation[];
  scopes?: ScopeRepresentation[];
  isExpanded: boolean;
};

export const PermissionsConfigurationTab = ({
  clientId,
}: PermissionsConfigurationProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const [permissions, setPermissions] =
    useState<ExpandablePolicyRepresentation[]>();
  const [selectedPermission, setSelectedPermission] =
    useState<PolicyRepresentation>();
  const [search, setSearch] = useState<SearchForm>({});
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [newDialog, toggleDialog] = useToggle();
  const resourceTypes = useSortedResourceTypes({ clientId });

  useFetch(
    async () => {
      const permissions = await adminClient.clients.listPermissionScope({
        first,
        max: max + 1,
        id: clientId,
        ...search,
      });

      const processedPermissions = await Promise.all(
        (permissions || []).map(async (permission) => {
          const policies = await adminClient.clients.getAssociatedPolicies({
            id: clientId,
            permissionId: permission.id!,
          });

          const scopes = await adminClient.clients.getAssociatedScopes({
            id: clientId,
            permissionId: permission.id!,
          });

          const resources = await adminClient.clients.getAssociatedResources({
            id: clientId,
            permissionId: permission.id!,
          });

          return {
            ...permission,
            policies,
            scopes,
            resources,
            isExpanded: false,
          };
        }),
      );

      return processedPermissions;
    },
    (permissions) => {
      setPermissions(permissions as any[]);
    },
    [key, search, first, max],
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deletePermission",
    messageKey: t("deleteAdminPermissionConfirm", {
      permission: selectedPermission?.name,
    }),
    continueButtonVariant: ButtonVariant.danger,
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delPermission({
          id: clientId,
          type: selectedPermission?.type!,
          permissionId: selectedPermission?.id!,
        });
        addAlert(t("permissionDeletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("permissionDeletedError", error);
      }
    },
  });

  if (!permissions) {
    return <KeycloakSpinner />;
  }

  const noData = permissions.length === 0;
  const searching = Object.keys(search).length !== 0;
  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
      <DeleteConfirm />
      {(!noData || searching) && (
        <>
          {newDialog && (
            <NewPermissionConfigurationDialog
              resourceTypes={resourceTypes}
              onSelect={(resourceType) =>
                navigate(
                  toCreatePermissionConfiguration({
                    realm,
                    permissionClientId: clientId,
                    resourceType: resourceType.type!,
                  }),
                )
              }
              toggleDialog={toggleDialog}
            />
          )}
          <PaginatingTableToolbar
            count={permissions.length}
            first={first}
            max={max}
            onNextClick={setFirst}
            onPreviousClick={setFirst}
            onPerPageSelect={(first, max) => {
              setFirst(first);
              setMax(max);
            }}
            toolbarItem={
              <>
                <ToolbarItem>
                  <SearchDropdown
                    types={resourceTypes}
                    search={search}
                    onSearch={setSearch}
                  />
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    data-testid="createScopeBasedPermissionBtn"
                    key="confirm"
                    variant="primary"
                    onSelect={(resourceType) =>
                      navigate(
                        toCreatePermissionConfiguration({
                          realm,
                          permissionClientId: clientId,
                          resourceType: resourceType.type!,
                        }),
                      )
                    }
                    onClick={toggleDialog}
                  >
                    {t("createPermission")}
                  </Button>
                </ToolbarItem>
              </>
            }
          >
            {!noData && (
              <Table aria-label={t("permissions")} variant="compact">
                <Thead>
                  <Tr>
                    <Th aria-hidden="true" />
                    <Th>{t("adminPermissionName")}</Th>
                    <Th>{t("resourceType")}</Th>
                    <Th>{t("authorizationScopes")}</Th>
                    <Th>{t("description")}</Th>
                    <Th aria-hidden="true" />
                  </Tr>
                </Thead>
                {permissions.map((permission, rowIndex) => (
                  <Tbody key={permission.id} isExpanded={permission.isExpanded}>
                    <Tr>
                      <Td
                        expand={{
                          rowIndex,
                          isExpanded: permission.isExpanded,
                          onToggle: (_, rowIndex) => {
                            const rows = permissions.map((p, index) =>
                              index === rowIndex
                                ? { ...p, isExpanded: !p.isExpanded }
                                : p,
                            );
                            setPermissions(rows);
                          },
                        }}
                      />
                      <Td data-testid={`name-column-${permission.name}`}>
                        <Link
                          to={toPermissionConfigurationDetails({
                            realm,
                            permissionClientId: clientId,
                            permissionId: permission.id!,
                            resourceType: permission.resourceType!,
                          })}
                        >
                          {permission.name}
                        </Link>
                      </Td>
                      <Td>{permission.resourceType}</Td>
                      <Td>
                        <AuthorizationScopesDetails
                          row={{
                            resourceType: permission.resourceType || "",
                            associatedScopes: permission.scopes?.map(
                              (scope: ScopeRepresentation) => ({
                                name: scope.name || "",
                              }),
                            ),
                          }}
                        />
                      </Td>
                      <Td>{permission.description || "—"}</Td>
                      <Td
                        actions={{
                          items: [
                            {
                              title: t("delete"),
                              onClick: async () => {
                                setSelectedPermission(permission);
                                toggleDeleteDialog();
                              },
                            },
                          ],
                        }}
                      ></Td>
                    </Tr>
                    <Tr
                      key={`child-${permission.id}`}
                      isExpanded={permission.isExpanded}
                    >
                      <Td />
                      <Td colSpan={5}>
                        <ExpandableRowContent>
                          {permission.isExpanded && (
                            <>
                              <Th>{t("resources")}</Th>
                              {permission.resources &&
                              permission.resources?.length > 0 ? (
                                permission.resources!.map(
                                  (resource: ResourceRepresentation, index) => (
                                    <Td key={index}>
                                      <span style={{ marginLeft: "8px" }}>
                                        {resource.displayName || resource.name}
                                      </span>
                                    </Td>
                                  ),
                                )
                              ) : (
                                <Td>
                                  <span style={{ marginLeft: "8px" }}>
                                    {t("allResources")}
                                  </span>
                                </Td>
                              )}
                              <br />
                              <Th>{t("assignedPolicies")}</Th>
                              {permission.policies!.map(
                                (policy: PolicyRepresentation, index) => (
                                  <Td key={index}>
                                    <span style={{ marginLeft: "8px" }}>
                                      {policy.name}
                                    </span>
                                  </Td>
                                ),
                              )}
                            </>
                          )}
                        </ExpandableRowContent>
                      </Td>
                    </Tr>
                  </Tbody>
                ))}
              </Table>
            )}
          </PaginatingTableToolbar>
        </>
      )}
      {noData && !searching && (
        <>
          {newDialog && (
            <NewPermissionConfigurationDialog
              resourceTypes={resourceTypes}
              onSelect={(resourceType) =>
                navigate(
                  toCreatePermissionConfiguration({
                    realm,
                    permissionClientId: clientId,
                    resourceType: resourceType.type!,
                  }),
                )
              }
              toggleDialog={toggleDialog}
            />
          )}
          <ListEmptyState
            message={t("emptyPermissions")}
            instructions={t("emptyPermissionsInstructions")}
            primaryActionText={t("createPermission")}
            onPrimaryAction={toggleDialog}
          />
        </>
      )}
      {noData && searching && (
        <ListEmptyState
          isSearchVariant
          message={t("noSearchResults")}
          instructions={t("noPermissionSearchResultsInstructions")}
        />
      )}
    </PageSection>
  );
};
