import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";

import {
  AlertVariant,
  Button,
  ButtonVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, To, useNavigate, useParams } from "react-router-dom";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";

import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import {
  Action,
  KeycloakDataTable,
} from "../../components/table-toolbar/KeycloakDataTable";

import { CustomUserFederationMapperRouteParams } from "../routes/CustomInstanceMapper";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

export type CustomInstanceMappersTabProps = {
  toPage: (mapperId?: string) => To;
};

type MapperLinkProps = ComponentRepresentation & {
  toPage: (mapperId: string) => To;
};

const MapperLink = ({ toPage, ...mapper }: MapperLinkProps) => (
  <Link to={toPage(mapper.id!)}>{mapper.name}</Link>
);

export const CustomInstanceMappersTab = ({
  toPage,
}: CustomInstanceMappersTabProps) => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const localeSort = useLocaleSort();
  const { addAlert, addError } = useAlerts();
  const { id, providerId } = useParams<CustomUserFederationMapperRouteParams>();

  const [key, setKey] = useState(0);
  const [mappers, setMappers] = useState<ComponentRepresentation[]>([]);
  const [selectedMapper, setSelectedMapper] =
    useState<ComponentRepresentation>();

  const refresh = () => setKey(key + 1);

  const provider = (
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.UserStorageProvider"
    ] || []
  ).find((p: ComponentTypeRepresentation) => p.id === providerId);

  useFetch(
    () => {
      return adminClient.components.find({
        parent: id,
        type: provider?.metadata.mapperType,
      });
    },
    (mappers: ComponentRepresentation[]) => {
      setMappers(
        localeSort(
          mappers.map((mapper) => ({
            ...mapper,
            name: mapper.name,
            type: mapper.providerId,
          })),
          mapByKey("name"),
        ),
      );
    },
    [key, provider],
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteMappingTitle", { mapperId: selectedMapper?.id }),
    messageKey: "deleteMappingConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({
          id: selectedMapper!.id!,
        });

        setSelectedMapper(undefined);
        refresh();

        addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("mappingDeletedError", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={key}
        loader={mappers}
        ariaLabelKey="ldapMappersList"
        searchPlaceholderKey="searchForMapper"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="add-mapper-btn"
              variant="primary"
              component={(props) => <Link {...props} to={toPage()} />}
            >
              {t("addMapper")}
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: t("delete"),
            onRowClick: (mapper) => {
              setSelectedMapper(mapper);
              toggleDeleteDialog();
            },
          } as Action<ComponentRepresentation>,
        ]}
        columns={[
          {
            name: "name",
            cellRenderer: (row) => <MapperLink {...row} toPage={toPage} />,
          },
          {
            name: "type",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyMappers")}
            instructions={t("emptyMappersInstructions")}
            primaryActionText={t("emptyPrimaryAction")}
            onPrimaryAction={() => navigate(toPage())}
          />
        }
      />
    </>
  );
};
