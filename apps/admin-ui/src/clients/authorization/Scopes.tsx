import { useState } from "react";
import { Link, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import {
  Button,
  DescriptionList,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  ExpandableRowContent,
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";

import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { PaginatingTableToolbar } from "../../components/table-toolbar/PaginatingTableToolbar";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { MoreLabel } from "./MoreLabel";
import { toScopeDetails } from "../routes/Scope";
import { toNewScope } from "../routes/NewScope";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import useToggle from "../../utils/useToggle";
import { DeleteScopeDialog } from "./DeleteScopeDialog";
import { DetailDescriptionLink } from "./DetailDescription";
import { toNewPermission } from "../routes/NewPermission";
import { toResourceDetails } from "../routes/Resource";
import { toPermissionDetails } from "../routes/PermissionDetails";

type ScopesProps = {
  clientId: string;
};

export type ExpandableScopeRepresentation = ScopeRepresentation & {
  permissions?: PolicyRepresentation[];
  isExpanded: boolean;
};

export const AuthorizationScopes = ({ clientId }: ScopesProps) => {
  const { t } = useTranslation("clients");
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const [deleteDialog, toggleDeleteDialog] = useToggle();
  const [scopes, setScopes] = useState<ExpandableScopeRepresentation[]>();
  const [selectedScope, setSelectedScope] =
    useState<ExpandableScopeRepresentation>();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState("");

  useFetch(
    async () => {
      const params = {
        first,
        max: max + 1,
        deep: false,
        name: search,
      };
      const scopes = await adminClient.clients.listAllScopes({
        ...params,
        id: clientId,
      });

      return await Promise.all(
        scopes.map(async (scope) => {
          const options = { id: clientId, scopeId: scope.id! };
          const [resources, permissions] = await Promise.all([
            adminClient.clients.listAllResourcesByScope(options),
            adminClient.clients.listAllPermissionsByScope(options),
          ]);

          return {
            ...scope,
            resources,
            permissions,
            isExpanded: false,
          };
        })
      );
    },
    setScopes,
    [key, search, first, max]
  );

  const ResourceRenderer = ({
    row,
  }: {
    row: ExpandableScopeRepresentation;
  }) => (
    <>
      {row.resources?.[0]?.name ? row.resources[0]?.name : "—"}{" "}
      <MoreLabel array={row.resources} />
    </>
  );

  const PermissionsRenderer = ({
    row,
  }: {
    row: ExpandableScopeRepresentation;
  }) => (
    <>
      {row.permissions?.[0]?.name ? row.permissions[0]?.name : "—"}{" "}
      <MoreLabel array={row.permissions} />
    </>
  );

  if (!scopes) {
    return <KeycloakSpinner />;
  }

  const noData = scopes.length === 0;
  const searching = search !== "";
  return (
    <PageSection variant="light" className="pf-u-p-0">
      <DeleteScopeDialog
        clientId={clientId}
        open={deleteDialog}
        toggleDialog={toggleDeleteDialog}
        selectedScope={selectedScope}
        refresh={refresh}
      />
      {(!noData || searching) && (
        <PaginatingTableToolbar
          count={scopes.length}
          first={first}
          max={max}
          onNextClick={setFirst}
          onPreviousClick={setFirst}
          onPerPageSelect={(first, max) => {
            setFirst(first);
            setMax(max);
          }}
          inputGroupName="search"
          inputGroupPlaceholder={t("searchByName")}
          inputGroupOnEnter={setSearch}
          toolbarItem={
            <ToolbarItem>
              <Button
                data-testid="createAuthorizationScope"
                component={(props) => (
                  <Link {...props} to={toNewScope({ realm, id: clientId })} />
                )}
              >
                {t("createAuthorizationScope")}
              </Button>
            </ToolbarItem>
          }
        >
          {!noData && (
            <TableComposable aria-label={t("scopes")} variant="compact">
              <Thead>
                <Tr>
                  <Th />
                  <Th>{t("common:name")}</Th>
                  <Th>{t("resources")}</Th>
                  <Th>{t("common:permissions")}</Th>
                  <Th />
                  <Th />
                </Tr>
              </Thead>
              {scopes.map((scope, rowIndex) => (
                <Tbody key={scope.id} isExpanded={scope.isExpanded}>
                  <Tr>
                    <Td
                      expand={{
                        rowIndex,
                        isExpanded: scope.isExpanded,
                        onToggle: (_, rowIndex) => {
                          const rows = scopes.map((resource, index) =>
                            index === rowIndex
                              ? {
                                  ...resource,
                                  isExpanded: !resource.isExpanded,
                                }
                              : resource
                          );
                          setScopes(rows);
                        },
                      }}
                    />
                    <Td data-testid={`name-column-${scope.name}`}>
                      <Link
                        to={toScopeDetails({
                          realm,
                          id: clientId,
                          scopeId: scope.id!,
                        })}
                      >
                        {scope.name}
                      </Link>
                    </Td>
                    <Td>
                      <ResourceRenderer row={scope} />
                    </Td>
                    <Td>
                      <PermissionsRenderer row={scope} />
                    </Td>
                    <Td width={10}>
                      <Button
                        variant="link"
                        component={(props) => (
                          <Link
                            {...props}
                            to={toNewPermission({
                              realm,
                              id: clientId,
                              permissionType: "scope",
                              selectedId: scope.id,
                            })}
                          />
                        )}
                      >
                        {t("createPermission")}
                      </Button>
                    </Td>
                    <Td
                      isActionCell
                      actions={{
                        items: [
                          {
                            title: t("common:delete"),
                            onClick: () => {
                              setSelectedScope(scope);
                              toggleDeleteDialog();
                            },
                          },
                        ],
                      }}
                    />
                  </Tr>
                  <Tr key={`child-${scope.id}`} isExpanded={scope.isExpanded}>
                    <Td />
                    <Td colSpan={4}>
                      <ExpandableRowContent>
                        {scope.isExpanded && (
                          <DescriptionList
                            isHorizontal
                            className="keycloak_resource_details"
                          >
                            <DetailDescriptionLink
                              name="resources"
                              array={scope.resources}
                              convert={(r) => r.name!}
                              link={(r) =>
                                toResourceDetails({
                                  id: clientId,
                                  realm,
                                  resourceId: r._id!,
                                })
                              }
                            />
                            <DetailDescriptionLink
                              name="associatedPermissions"
                              array={scope.permissions}
                              convert={(p) => p.name!}
                              link={(p) =>
                                toPermissionDetails({
                                  id: clientId,
                                  realm,
                                  permissionId: p.id!,
                                  permissionType: p.type!,
                                })
                              }
                            />
                          </DescriptionList>
                        )}
                      </ExpandableRowContent>
                    </Td>
                  </Tr>
                </Tbody>
              ))}
            </TableComposable>
          )}
        </PaginatingTableToolbar>
      )}
      {noData && !searching && (
        <ListEmptyState
          message={t("emptyAuthorizationScopes")}
          instructions={t("emptyAuthorizationInstructions")}
          onPrimaryAction={() => navigate(toNewScope({ id: clientId, realm }))}
          primaryActionText={t("createAuthorizationScope")}
        />
      )}
      {noData && searching && (
        <ListEmptyState
          isSearchVariant
          message={t("common:noSearchResults")}
          instructions={t("common:noSearchResultsInstructions")}
        />
      )}
    </PageSection>
  );
};
