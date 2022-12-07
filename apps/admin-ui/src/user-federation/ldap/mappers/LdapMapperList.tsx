import { useState } from "react";
import { useRouteMatch } from "react-router-dom";
import { useParams, Link, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  ToolbarItem,
} from "@patternfly/react-core";

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { KeycloakDataTable } from "../../../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { useAlerts } from "../../../components/alert/Alerts";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { useConfirmDialog } from "../../../components/confirm-dialog/ConfirmDialog";
import useLocaleSort, { mapByKey } from "../../../utils/useLocaleSort";

export const LdapMapperList = () => {
  const navigate = useNavigate();
  const { t } = useTranslation("user-federation");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { url } = useRouteMatch();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [mappers, setMappers] = useState<ComponentRepresentation[]>([]);
  const localeSort = useLocaleSort();

  const { id } = useParams<{ id: string }>();

  const [selectedMapper, setSelectedMapper] =
    useState<ComponentRepresentation>();

  useFetch(
    () =>
      adminClient.components.find({
        parent: id,
        type: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
      }),
    (mapper) => {
      setMappers(
        localeSort(
          mapper.map((mapper) => ({
            ...mapper,
            name: mapper.name,
            type: mapper.providerId,
          })),
          mapByKey("name")
        )
      );
    },
    [key]
  );

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
        addError("common:mappingDeletedError", error);
      }
    },
  });

  const getUrl = (url: string) => {
    if (!url.includes("/mappers")) {
      return `${url}/mappers`;
    }
    return `${url}`;
  };

  const MapperLink = (mapper: ComponentRepresentation) => (
    <Link to={`${getUrl(url)}/${mapper.id}`}>{mapper.name}</Link>
  );

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={key}
        loader={mappers}
        ariaLabelKey="ldapMappersList"
        searchPlaceholderKey="common:searchForMapper"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="add-mapper-btn"
              variant="primary"
              onClick={() => navigate(`${url}/new`)}
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
            onPrimaryAction={() => navigate(`${url}/new`)}
          />
        }
      />
    </>
  );
};
