import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Alert,
  AlertVariant,
  Button,
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

import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { PaginatingTableToolbar } from "../../components/table-toolbar/PaginatingTableToolbar";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { DetailCell } from "./DetailCell";
import { toCreateResource } from "../routes/NewResource";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toResourceDetails } from "../routes/Resource";
import { MoreLabel } from "./MoreLabel";

type ResourcesProps = {
  clientId: string;
};

type ExpandableResourceRepresentation = ResourceRepresentation & {
  isExpanded: boolean;
};

export const AuthorizationResources = ({ clientId }: ResourcesProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
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

  useFetch(
    () => {
      const params = {
        first,
        max,
        deep: false,
      };
      return adminClient.clients.listResources({
        ...params,
        id: clientId,
      });
    },
    (resources) =>
      setResources(
        resources.map((resource) => ({ ...resource, isExpanded: false }))
      ),
    [key]
  );

  const UriRenderer = ({ row }: { row: ResourceRepresentation }) => (
    <>
      {row.uris?.[0]} <MoreLabel array={row.uris} />
    </>
  );

  const fetchPermissions = async (id: string) => {
    return adminClient.clients.listPermissionsByResource({
      id: clientId,
      resourceId: id,
    });
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:deleteResource",
    children: (
      <>
        {t("deleteResourceConfirm")}
        {permissions?.length && (
          <Alert
            variant="warning"
            isInline
            isPlain
            title={t("deleteResourceWarning")}
            className="pf-u-pt-lg"
          >
            <p className="pf-u-pt-xs">
              {permissions.map((permission) => (
                <strong key={permission.id} className="pf-u-pr-md">
                  {permission.name}
                </strong>
              ))}
            </p>
          </Alert>
        )}
      </>
    ),
    continueButtonLabel: "clients:confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delResource({
          id: clientId,
          resourceId: selectedResource?._id!,
        });
        addAlert(t("resourceDeletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("clients:resourceDeletedError", error);
      }
    },
  });

  if (!resources) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light" className="pf-u-p-0">
      <DeleteConfirm />
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
          <ToolbarItem>
            <Button
              data-testid="createResource"
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
        }
      >
        <TableComposable aria-label={t("resources")} variant="compact">
          <Thead>
            <Tr>
              <Th />
              <Th>{t("common:name")}</Th>
              <Th>{t("common:type")}</Th>
              <Th>{t("owner")}</Th>
              <Th>{t("uris")}</Th>
              <Th />
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
                          ? { ...resource, isExpanded: !resource.isExpanded }
                          : resource
                      );
                      setResources(rows);
                    },
                  }}
                />
                <Td data-testid={`name-column-${resource.name}`}>
                  <Link
                    to={toResourceDetails({
                      realm,
                      id: clientId,
                      resourceId: resource._id!,
                    })}
                  >
                    {resource.name}
                  </Link>
                </Td>
                <Td>{resource.type}</Td>
                <Td>{resource.owner?.name}</Td>
                <Td>
                  <UriRenderer row={resource} />
                </Td>
                <Td
                  actions={{
                    items: [
                      {
                        title: t("common:delete"),
                        onClick: async () => {
                          setSelectedResource(resource);
                          setPermission(await fetchPermissions(resource._id!));
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
              <Tr
                key={`child-${resource._id}`}
                isExpanded={resource.isExpanded}
              >
                <Td colSpan={5}>
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
        </TableComposable>
      </PaginatingTableToolbar>
    </PageSection>
  );
};
