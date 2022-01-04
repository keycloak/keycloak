import React, { useState } from "react";
import { Link, useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
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

type ScopesProps = {
  clientId: string;
};

export type ExpandableScopeRepresentation = ScopeRepresentation & {
  permissions?: PolicyRepresentation[];
  isExpanded: boolean;
};

export const AuthorizationScopes = ({ clientId }: ScopesProps) => {
  const { t } = useTranslation("clients");
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const [deleteDialog, toggleDeleteDialog] = useToggle();
  const [scopes, setScopes] = useState<ExpandableScopeRepresentation[]>();
  const [selectedScope, setSelectedScope] =
    useState<ExpandableScopeRepresentation>();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);

  useFetch(
    async () => {
      const params = {
        first,
        max,
        deep: false,
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
    [key]
  );

  const ResourceRenderer = ({
    row,
  }: {
    row: ExpandableScopeRepresentation;
  }) => {
    return (
      <>
        {row.resources?.[0]?.name} <MoreLabel array={row.resources} />
      </>
    );
  };

  const PermissionsRenderer = ({
    row,
  }: {
    row: ExpandableScopeRepresentation;
  }) => {
    return (
      <>
        {row.permissions?.[0]?.name} <MoreLabel array={row.permissions} />
      </>
    );
  };

  if (!scopes) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light" className="pf-u-p-0">
      <DeleteScopeDialog
        clientId={clientId}
        open={deleteDialog}
        toggleDialog={toggleDeleteDialog}
        selectedScope={selectedScope}
        refresh={refresh}
      />
      {scopes.length > 0 && (
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
          <TableComposable aria-label={t("scopes")} variant="compact">
            <Thead>
              <Tr>
                <Th />
                <Th>{t("common:name")}</Th>
                <Th>{t("resources")}</Th>
                <Th>{t("permissions")}</Th>
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
                            ? { ...resource, isExpanded: !resource.isExpanded }
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
                  <Td
                    actions={{
                      items: [
                        {
                          title: t("common:delete"),
                          onClick: () => {
                            setSelectedScope(scope);
                            toggleDeleteDialog();
                          },
                        },
                        {
                          title: t("createPermission"),
                          className: "pf-m-link",
                          isOutsideDropdown: true,
                        },
                      ],
                    }}
                  />
                </Tr>
                <Tr key={`child-${scope.id}`} isExpanded={scope.isExpanded}>
                  <Td colSpan={5}>
                    <ExpandableRowContent>
                      {scope.isExpanded && (
                        <DescriptionList
                          isHorizontal
                          className="keycloak_resource_details"
                        >
                          <DescriptionListGroup>
                            <DescriptionListTerm>
                              {t("resources")}
                            </DescriptionListTerm>
                            <DescriptionListDescription>
                              {scope.resources?.map((resource) => (
                                <span key={resource._id} className="pf-u-pr-sm">
                                  {resource.name}
                                </span>
                              ))}
                              {scope.resources?.length === 0 && (
                                <i>{t("common:none")}</i>
                              )}
                            </DescriptionListDescription>
                          </DescriptionListGroup>
                          <DescriptionListGroup>
                            <DescriptionListTerm>
                              {t("associatedPermissions")}
                            </DescriptionListTerm>
                            <DescriptionListDescription>
                              {scope.permissions?.map((permission) => (
                                <span
                                  key={permission.id}
                                  className="pf-u-pr-sm"
                                >
                                  {permission.name}
                                </span>
                              ))}
                              {scope.permissions?.length === 0 && (
                                <i>{t("common:none")}</i>
                              )}
                            </DescriptionListDescription>
                          </DescriptionListGroup>
                        </DescriptionList>
                      )}
                    </ExpandableRowContent>
                  </Td>
                </Tr>
              </Tbody>
            ))}
          </TableComposable>
        </PaginatingTableToolbar>
      )}
      {scopes.length === 0 && (
        <ListEmptyState
          message={t("emptyAuthorizationScopes")}
          instructions={t("emptyAuthorizationInstructions")}
          onPrimaryAction={() =>
            history.push(toNewScope({ id: clientId, realm }))
          }
          primaryActionText={t("createAuthorizationScope")}
        />
      )}
    </PageSection>
  );
};
