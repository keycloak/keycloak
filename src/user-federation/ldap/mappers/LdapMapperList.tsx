import React, { useState } from "react";
import { Link, useHistory, useParams, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  ToolbarItem,
} from "@patternfly/react-core";

import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { KeycloakDataTable } from "../../../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { useAlerts } from "../../../components/alert/Alerts";
import { useAdminClient } from "../../../context/auth/AdminClient";
import { useConfirmDialog } from "../../../components/confirm-dialog/ConfirmDialog";

export const LdapMapperList = () => {
  const history = useHistory();
  const { t } = useTranslation("user-federation");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const { url } = useRouteMatch();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const { id } = useParams<{ id: string }>();

  const [selectedMapper, setSelectedMapper] = useState<
    ComponentRepresentation
  >();

  const loader = async () => {
    const testParams: {
      [name: string]: string | number;
    } = {
      parent: id,
      type: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
    };

    const mappersList = (await adminClient.components.find(testParams)).map(
      (mapper) => {
        return {
          ...mapper,
          name: mapper.name,
          type: mapper.providerId,
        } as ComponentRepresentation;
      }
    );
    return mappersList;
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("common:deleteMappingTitle", { mapperId: selectedMapper?.id }),
    messageKey: "common:deleteMappingConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({
          id: selectedMapper!.id!,
        });
        refresh();
        addAlert(t("common:mappingDeletedSuccess"), AlertVariant.success);
        setSelectedMapper(undefined);
      } catch (error) {
        addAlert(
          t("common:mappingDeletedError", { error }),
          AlertVariant.danger
        );
      }
    },
  });

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
      <DeleteConfirm />
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="ldapMappersList"
        searchPlaceholderKey="common:searchForMapper"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="add-mapper-btn"
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
            onRowClick: (mapper) => {
              setSelectedMapper(mapper);
              toggleDeleteDialog();
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
            onPrimaryAction={() => history.push(`${url}/new`)}
          />
        }
      />
    </>
  );
};
