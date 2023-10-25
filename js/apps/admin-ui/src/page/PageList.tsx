import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type { ComponentQuery } from "@keycloak/keycloak-admin-client/lib/resources/components";
import {
  Button,
  ButtonVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { adminClient } from "../admin-client";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useFetch } from "../utils/useFetch";
import { toDetailPage } from "./routes";
import { useTranslation } from "react-i18next";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../components/alert/Alerts";
import { IRowData } from "@patternfly/react-table";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";

export const PAGE_PROVIDER = "org.keycloak.services.ui.extend.UiPageProvider";

const DetailLink = (obj: ComponentRepresentation) => {
  const { realm } = useRealm();
  return (
    <Link
      key={obj.id}
      to={toDetailPage({ realm, providerId: obj.providerId!, id: obj.id! })}
    >
      {obj.id}
    </Link>
  );
};
export default function PageList() {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [selectedItem, setSelectedItem] = useState<ComponentRepresentation>();
  const { componentTypes } = useServerInfo();
  const pages = componentTypes?.[PAGE_PROVIDER];

  // Here the providerId should be used instead
  const page = pages?.[0]!;

  useFetch(
    async () => adminClient.realms.findOne({ realm: realmName }),
    setRealm,
    [],
  );

  const loader = async () => {
    const params: ComponentQuery = {
      parent: realm?.id,
      type: PAGE_PROVIDER,
    };
    return await adminClient.components.find({ ...params });
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "itemDeleteConfirmTitle",
    messageKey: "itemDeleteConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({
          id: selectedItem!.id!,
        });
        addAlert(t("itemDeletedSuccess"));
        refresh();
      } catch (error) {
        addError("clientDeleteError", error);
      }
    },
  });

  return (
    <PageSection variant="light" className="pf-u-p-0">
      <DeleteConfirm />
      <ViewHeader titleKey={page.id} subKey={page.helpText} divider={false} />
      <KeycloakDataTable
        key={key}
        toolbarItem={
          <ToolbarItem>
            <Button
              component={(props) => (
                <Link
                  {...props}
                  to={toDetailPage({ realm: realmName, providerId: page.id })}
                />
              )}
            >
              {t("createItem")}
            </Button>
          </ToolbarItem>
        }
        actionResolver={(item: IRowData) => [
          {
            title: t("delete"),
            onClick() {
              setSelectedItem(item.data);
              toggleDeleteDialog();
            },
          },
        ]}
        searchPlaceholderKey="searchItem"
        loader={loader}
        columns={[
          { name: "id", cellRenderer: DetailLink },
          ...page.properties.slice(0, 3).map((p) => ({
            name: `config.${p.name}[0]`,
            displayKey: p.name,
          })),
        ]}
        ariaLabelKey="list"
        emptyState={
          <ListEmptyState
            hasIcon
            message={t("noItems")}
            instructions={t("noItemsInstructions")}
            primaryActionText={t("createItem")}
            onPrimaryAction={() =>
              navigate(toDetailPage({ realm: realmName, providerId: page.id }))
            }
          />
        }
      />
    </PageSection>
  );
}
