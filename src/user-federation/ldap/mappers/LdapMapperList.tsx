import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant, Button, ToolbarItem } from "@patternfly/react-core";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { useErrorHandler } from "react-error-boundary";
import { KeycloakDataTable } from "../../../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { useAlerts } from "../../../components/alert/Alerts";
import {
  useAdminClient,
  asyncStateFetch,
} from "../../../context/auth/AdminClient";
import { Link, useParams, useRouteMatch } from "react-router-dom";

export const LdapMapperList = () => {
  const [mappers, setMappers] = useState<ComponentRepresentation[]>();

  const { t } = useTranslation("user-federation");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const { url } = useRouteMatch();

  const handleError = useErrorHandler();
  const [key, setKey] = useState(0);

  const { id } = useParams<{ id: string }>();

  useEffect(() => {
    return asyncStateFetch(
      () => {
        const testParams: {
          [name: string]: string | number;
        } = {
          parent: id,
          type: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
        };
        return adminClient.components.find(testParams);
      },
      (mappers) => {
        setMappers(mappers);
        // TODO: remove after debugging
        console.log("LdapMapperList - setMappers being set with:");
        console.log(mappers);
      },
      handleError
    );
  }, [key]);

  if (!mappers) {
    return (
      <>
        <ListEmptyState
          message={t("common:emptyMappers")}
          instructions={t("common:emptyMappersInstructions")}
          primaryActionText={t("common:emptyPrimaryAction")}
        />
      </>
    );
  }

  const loader = async () =>
    Promise.resolve(
      (mappers || []).map((mapper) => {
        return {
          ...mapper,
          name: mapper.name,
          type: mapper.providerId,
        } as ComponentRepresentation;
      })
    );

  const getUrl = (url: string) => {
    if (url.indexOf("/mappers") === -1) {
      return `${url}/mappers`;
    }
    return `${url}`;
  };

  const MapperLink = (mapper: ComponentRepresentation) => (
    <>
      <Link to={`${getUrl(url)}/${mapper.id}`}>{mapper.name}</Link>
    </>
  );

  return (
    <>
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="ldapMappersList"
        searchPlaceholderKey="common:searchForMapper"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="createMapperBtn"
              variant="primary"
              // onClick={handleModalToggle}
              onClick={() =>
                addAlert(
                  t("Add functionality not implemented yet!"),
                  AlertVariant.success
                )
              }
            >
              {t("common:addMapper")}
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: t("common:delete"),
            onRowClick: async (mapper) => {
              try {
                addAlert(
                  // t("common:mappingDeletedError"),
                  "Delete functionality not implemented yet!",
                  AlertVariant.success
                );
              } catch (error) {
                addAlert(
                  t("common:mappingDeletedError", { error }),
                  AlertVariant.danger
                );
              }
              return true;
            },
          },
        ]}
        columns={[
          {
            name: "name",
            cellRenderer: MapperLink,
          },
          {
            name: "type",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("common:emptyMappers")}
            instructions={t("common:emptyMappersInstructions")}
            primaryActionText={t("common:emptyPrimaryAction")}
          />
        }
      />
    </>
  );
};
