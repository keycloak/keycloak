import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import {
  ListEmptyState,
  PaginatingTableToolbar,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  AlertVariant,
  Button,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  ExpandableRowContent,
  Table,
  TableText,
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
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toNewPermission } from "../routes/NewPermission";
import { toCreateResource } from "../routes/NewResource";
import { toResourceDetails } from "../routes/Resource";
import { DetailCell } from "./DetailCell";
import { MoreLabel } from "./MoreLabel";
import { SearchDropdown, SearchForm } from "./SearchDropdown";

type ResourcesProps = {
  clientId: string;
  isDisabled?: boolean;
};

type ExpandableResourceRepresentation = ResourceRepresentation & {
  isExpanded: boolean;
};

const UriRenderer = ({ row }: { row: ResourceRepresentation }) => (
  <TableText wrapModifier="truncate">
    {row.uris?.[0]} <MoreLabel array={row.uris} />
  </TableText>
);

export const AuthorizationResources = ({
  clientId,
  isDisabled = false,
}: ResourcesProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const [resources, setResources] =
    useState<ExpandableResourceRepresentation[]>();
  const [selectedResource, setSelectedResource] =
    useState<ResourceRepresentation>();
  const [permissions, setPermission] =
    useState<ResourceServerRepresentation[]>();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState<SearchForm>({});

  useFetch(
    () => {
      const params = {
        first,
        max: max + 1,
        deep: false,
        ...search,
      };
      return adminClient.clients.listResources({
        ...params,
        id: clientId,
      });
    },
    (resources) =>
      setResources(
        resources.map((resource) => ({ ...resource, isExpanded: false })),
      ),
    [key, search, first, max],
  );

  const fetchPermissions = async (id: string) => {
    return adminClient.clients.listPermissionsByResource({
      id: clientId,
      resourceId: id,
    });
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteResource",
    children: (
      <>
        {t("deleteResourceConfirm")}
        {permissions?.length && (
          <Alert
            variant="warning"
            isInline
            isPlain
            title={t("deleteResourceWarning")}
            className="pf-v5-u-pt-lg"
          >
            <p className="pf-v5-u-pt-xs">
              {permissions.map((permission) => (
                <strong key={permission.id} className="pf-v5-u-pr-md">
                  {permission.name}
                </strong>
              ))}
            </p>
          </Alert>
        )}
      </>
    ),
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delResource({
          id: clientId,
          resourceId: selectedResource?._id!,
        });
        addAlert(t("resourceDeletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("resourceDeletedError", error);
      }
    },
  });

  if (!resources) {
    return <KeycloakSpinner />;
  }

  const noData = resources.length === 0;
  const searching = Object.keys(search).length !== 0;
  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
      <DeleteConfirm />
      {(!noData || searching) && (
        <PaginatingTableToolbar
          count={resources.length}
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
                  search={search}
                  onSearch={setSearch}
                  type="resource"
                />
              </ToolbarItem>

              <ToolbarItem>
                <Button
                  data-testid="createResource"
                  isDisabled={isDisabled}
                  component={(props) => (
                    <Link
                      {...props}
                      to={toCreateResource({ realm, id: clientId })}
                    />
                  )}
                >
                  {t("createResource")}
                </Button>
              </ToolbarItem>
            </>
          }
        >
          {!noData && (
            <Table aria-label={t("resources")} variant="compact">
              <Thead>
                <Tr>
                  <Th aria-hidden="true" />
                  <Th>{t("name")}</Th>
                  <Th>{t("displayName")}</Th>
                  <Th>{t("type")}</Th>
                  <Th>{t("owner")}</Th>
                  <Th>{t("uris")}</Th>
                  {!isDisabled && (
                    <>
                      <Th aria-hidden="true" />
                      <Th aria-hidden="true" />
                    </>
                  )}
                </Tr>
              </Thead>
              {resources.map((resource, rowIndex) => (
                <Tbody key={resource._id} isExpanded={resource.isExpanded}>
                  <Tr>
                    <Td
                      expand={{
                        rowIndex,
                        isExpanded: resource.isExpanded,
                        onToggle: (_, rowIndex) => {
                          const rows = resources.map((resource, index) =>
                            index === rowIndex
                              ? {
                                  ...resource,
                                  isExpanded: !resource.isExpanded,
                                }
                              : resource,
                          );
                          setResources(rows);
                        },
                      }}
                    />
                    <Td data-testid={`name-column-${resource.name}`}>
                      <TableText wrapModifier="truncate">
                        <Link
                          to={toResourceDetails({
                            realm,
                            id: clientId,
                            resourceId: resource._id!,
                          })}
                        >
                          {resource.name}
                        </Link>
                      </TableText>
                    </Td>
                    <Td>
                      <TableText wrapModifier="truncate">
                        {resource.displayName}
                      </TableText>
                    </Td>
                    <Td>
                      <TableText wrapModifier="truncate">
                        {resource.type}
                      </TableText>
                    </Td>
                    <Td>
                      <TableText wrapModifier="truncate">
                        {resource.owner?.name}
                      </TableText>
                    </Td>
                    <Td>
                      <UriRenderer row={resource} />
                    </Td>
                    {!isDisabled && (
                      <>
                        <Td width={10}>
                          <Button
                            variant="link"
                            component={(props) => (
                              <Link
                                {...props}
                                to={toNewPermission({
                                  realm,
                                  id: clientId,
                                  permissionType: "resource",
                                  selectedId: resource._id,
                                })}
                              />
                            )}
                          >
                            {t("createPermission")}
                          </Button>
                        </Td>
                        <Td
                          actions={{
                            items: [
                              {
                                title: t("delete"),
                                onClick: async () => {
                                  setSelectedResource(resource);
                                  setPermission(
                                    await fetchPermissions(resource._id!),
                                  );
                                  toggleDeleteDialog();
                                },
                              },
                            ],
                          }}
                        />
                      </>
                    )}
                  </Tr>
                  <Tr
                    key={`child-${resource._id}`}
                    isExpanded={resource.isExpanded}
                  >
                    <Td />
                    <Td colSpan={4}>
                      <ExpandableRowContent>
                        {resource.isExpanded && (
                          <DetailCell
                            clientId={clientId}
                            id={resource._id!}
                            uris={resource.uris}
                          />
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
      {noData && searching && (
        <ListEmptyState
          isSearchVariant
          message={t("noSearchResults")}
          instructions={t("noSearchResultsInstructions")}
        />
      )}
      {noData && !searching && (
        <ListEmptyState
          message={t("emptyResources")}
          instructions={t("emptyResourcesInstructions")}
          isDisabled={isDisabled}
          primaryActionText={t("createResource")}
          onPrimaryAction={() =>
            navigate(toCreateResource({ realm, id: clientId }))
          }
        />
      )}
    </PageSection>
  );
};
