import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ToolbarItem,
} from "@patternfly/react-core";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { useErrorHandler } from "react-error-boundary";
import { KeycloakDataTable } from "../../../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { useAlerts } from "../../../components/alert/Alerts";
import {
  useAdminClient,
  asyncStateFetch,
} from "../../../context/auth/AdminClient";
import { Link, useHistory, useParams, useRouteMatch } from "react-router-dom";

export const LdapMapperList = () => {
  const [mappers, setMappers] = useState<ComponentRepresentation[]>();
  const history = useHistory();
  const { t } = useTranslation("user-federation");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const { url } = useRouteMatch();
  const handleError = useErrorHandler();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
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

  const deleteMapper = async (mapper: ComponentRepresentation) => {
    try {
      await adminClient.components.del({
        id: mapper.id!,
      });
      // refresh();
      addAlert(t("mappingDelete"), AlertVariant.success);
    } catch (error) {
      addAlert(t("mappingDeleteError", { error }), AlertVariant.danger);
    }
    return true;
  };

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
              onClick={() => history.push(`${url}/new`)}
            >
              {t("common:addMapper")}
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: t("common:delete"),
            onRowClick: async (mapper: ComponentRepresentation) => {
              await deleteMapper(mapper);
              refresh();
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
