import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import {
  ListEmptyState,
  PaginatingTableToolbar,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  DescriptionList,
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
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import useToggle from "../../utils/useToggle";
import { toNewPermission } from "../routes/NewPermission";
import { toNewScope } from "../routes/NewScope";
import { toPermissionDetails } from "../routes/PermissionDetails";
import { toResourceDetails } from "../routes/Resource";
import { toScopeDetails } from "../routes/Scope";
import { DeleteScopeDialog } from "./DeleteScopeDialog";
import { DetailDescriptionLink } from "./DetailDescription";

type ScopesProps = {
  clientId: string;
  isDisabled?: boolean;
};

export type PermissionScopeRepresentation = ScopeRepresentation & {
  permissions?: PolicyRepresentation[];
  isLoaded: boolean;
};

type ExpandableRow = {
  id: string;
  isExpanded: boolean;
};

export const AuthorizationScopes = ({
  clientId,
  isDisabled = false,
}: ScopesProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { realm } = useRealm();

  const [deleteDialog, toggleDeleteDialog] = useToggle();
  const [scopes, setScopes] = useState<PermissionScopeRepresentation[]>();
  const [selectedScope, setSelectedScope] =
    useState<PermissionScopeRepresentation>();
  const [collapsed, setCollapsed] = useState<ExpandableRow[]>([]);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState("");

  useFetch(
    () => {
      const params = {
        first,
        max: max + 1,
        deep: false,
        name: search,
      };
      return adminClient.clients.listAllScopes({
        ...params,
        id: clientId,
      });
    },
    (scopes) => {
      setScopes(scopes.map((s) => ({ ...s, isLoaded: false })));
      setCollapsed(scopes.map((s) => ({ id: s.id!, isExpanded: false })));
    },
    [key, search, first, max],
  );

  const getScope = (id: string) => scopes?.find((scope) => scope.id === id)!;
  const isExpanded = (id: string | undefined) =>
    collapsed.find((c) => c.id === id)?.isExpanded || false;

  useFetch(
    () => {
      const newlyOpened = collapsed
        .filter((row) => row.isExpanded)
        .map(({ id }) => getScope(id))
        .filter((s) => !s.isLoaded);

      return Promise.all(
        newlyOpened.map(async (scope) => {
          const [resources, permissions] = await Promise.all([
            adminClient.clients.listAllResourcesByScope({
              id: clientId,
              scopeId: scope.id!,
            }),
            adminClient.clients.listAllPermissionsByScope({
              id: clientId,
              scopeId: scope.id!,
            }),
          ]);

          return {
            ...scope,
            resources,
            permissions,
            isLoaded: true,
          };
        }),
      );
    },
    (resourcesScopes) => {
      let result = [...(scopes || [])];
      resourcesScopes.forEach((resourceScope) => {
        const index = scopes?.findIndex(
          (scope) => resourceScope.id === scope.id,
        )!;
        result = [
          ...result.slice(0, index),
          resourceScope,
          ...result.slice(index + 1),
        ];
      });

      setScopes(result);
    },
    [collapsed],
  );

  if (!scopes) {
    return <KeycloakSpinner />;
  }

  const noData = scopes.length === 0;
  const searching = search !== "";
  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
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
            <Table aria-label={t("scopes")} variant="compact">
              <Thead>
                <Tr>
                  <Th aria-hidden="true" />
                  <Th>{t("name")}</Th>
                  <Th>{t("displayName")}</Th>
                  <Th aria-hidden="true" />
                  <Th aria-hidden="true" />
                </Tr>
              </Thead>
              {scopes.map((scope, rowIndex) => (
                <Tbody key={scope.id} isExpanded={isExpanded(scope.id)}>
                  <Tr>
                    <Td
                      expand={{
                        rowIndex,
                        isExpanded: isExpanded(scope.id),
                        onToggle: (_event, index, isExpanded) => {
                          setCollapsed([
                            ...collapsed.slice(0, index),
                            { id: scope.id!, isExpanded },
                            ...collapsed.slice(index + 1),
                          ]);
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
                    <Td>{scope.displayName}</Td>
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
                            title: t("delete"),
                            onClick: () => {
                              setSelectedScope(scope);
                              toggleDeleteDialog();
                            },
                          },
                        ],
                      }}
                    />
                  </Tr>
                  <Tr
                    key={`child-${scope.id}`}
                    isExpanded={isExpanded(scope.id)}
                  >
                    <Td />
                    <Td colSpan={4}>
                      <ExpandableRowContent>
                        {isExpanded(scope.id) && scope.isLoaded ? (
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
                        ) : (
                          <KeycloakSpinner />
                        )}
                      </ExpandableRowContent>
                    </Td>
                  </Tr>
                </Tbody>
              ))}
            </Table>
          )}
        </PaginatingTableToolbar>
      )}
      {noData && !searching && (
        <ListEmptyState
          message={t("emptyAuthorizationScopes")}
          instructions={t("emptyAuthorizationInstructions")}
          isDisabled={isDisabled}
          onPrimaryAction={() => navigate(toNewScope({ id: clientId, realm }))}
          primaryActionText={t("createAuthorizationScope")}
        />
      )}
      {noData && searching && (
        <ListEmptyState
          isSearchVariant
          isDisabled={isDisabled}
          message={t("noSearchResults")}
          instructions={t("noSearchResultsInstructions")}
        />
      )}
    </PageSection>
  );
};
