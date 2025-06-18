import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type { ComponentQuery } from "@keycloak/keycloak-admin-client/lib/resources/components";
import {
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { IRowData } from "@patternfly/react-table";
import { get } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { PAGE_PROVIDER } from "./constants";
import { addDetailPage, PageListParams, toDetailPage } from "./routes";

type DetailLinkProps = {
  obj: ComponentRepresentation;
  field: string;
};

const DetailLink = ({ obj, field }: DetailLinkProps) => {
  const { realm } = useRealm();
  const value = get(obj, field);
  return (
    <Link
      key={value}
      to={toDetailPage({ realm, providerId: obj.providerId!, id: obj.id! })}
    >
      {value}
    </Link>
  );
};
export default function PageList() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { providerId } = useParams<PageListParams>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const [selectedItem, setSelectedItem] = useState<ComponentRepresentation>();
  const { componentTypes } = useServerInfo();
  const pages = componentTypes?.[PAGE_PROVIDER];

  const page = pages?.find((p) => p.id === providerId)!;

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
        addError("itemSaveError", error);
      }
    },
  });

  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
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
                  to={addDetailPage({ realm: realmName, providerId: page.id })}
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
          ...(
            page.metadata.displayFields ||
            page.properties.slice(0, 3).map((p) => p.name)
          ).map((name: string, index: number) => ({
            name: `config.${name}[0]`,
            displayKey: page.properties.find((p) => p.name === name)!.label,
            cellRenderer:
              index === 0
                ? (obj: ComponentRepresentation) => (
                    <DetailLink obj={obj} field={`config.${name}`} />
                  )
                : undefined,
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
              navigate(addDetailPage({ realm: realmName, providerId: page.id }))
            }
          />
        }
      />
    </PageSection>
  );
}
