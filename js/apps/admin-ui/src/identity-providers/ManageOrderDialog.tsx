import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  Button,
  ButtonVariant,
  DataList,
  DataListCell,
  DataListControl,
  DataListDragButton,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  ModalVariant,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { sortBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useFetch } from "../utils/useFetch";

type ManageOrderDialogProps = {
  onClose: () => void;
};

export const ManageOrderDialog = ({ onClose }: ManageOrderDialogProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [alias, setAlias] = useState("");
  const [liveText, setLiveText] = useState("");
  const [providers, setProviders] =
    useState<IdentityProviderRepresentation[]>();
  const [order, setOrder] = useState<string[]>([]);

  const onDragStart = (id: string) => {
    setAlias(id);
    setLiveText(t("onDragStart", { item: id }));
  };

  const onDragMove = () => {
    setLiveText(t("onDragMove", { item: alias }));
  };

  const onDragCancel = () => {
    setLiveText(t("onDragCancel"));
  };

  const onDragFinish = (providerOrder: string[]) => {
    setLiveText(t("onDragFinish", { list: providerOrder }));
    setOrder(providerOrder);
  };

  useFetch(
    () => adminClient.identityProviders.find(),
    (providers) => {
      setProviders(sortBy(providers, ["config.guiOrder", "alias"]));
      setOrder(providers.map((provider) => provider.alias!));
    },
    [],
  );

  if (!providers) {
    return <KeycloakSpinner />;
  }

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("manageDisplayOrder")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          id="modal-confirm"
          data-testid="confirm"
          key="confirm"
          onClick={async () => {
            const updates = order.map((alias, index) => {
              const provider = providers.find((p) => p.alias === alias)!;
              provider.config!.guiOrder = index;
              return adminClient.identityProviders.update({ alias }, provider);
            });

            try {
              await Promise.all(updates);
              addAlert(t("orderChangeSuccess"));
            } catch (error) {
              addError("orderChangeError", error);
            }

            onClose();
          }}
        >
          {t("save")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <TextContent className="pf-u-pb-lg">
        <Text>{t("orderDialogIntro")}</Text>
      </TextContent>

      <DataList
        aria-label={t("manageOrderTableAria")}
        data-testid="manageOrderDataList"
        isCompact
        onDragFinish={onDragFinish}
        onDragStart={onDragStart}
        onDragMove={onDragMove}
        onDragCancel={onDragCancel}
        itemOrder={order}
      >
        {sortBy(providers, "config.guiOrder", "alias").map((provider) => (
          <DataListItem
            aria-label={provider.alias}
            id={provider.alias}
            key={provider.alias}
          >
            <DataListItemRow>
              <DataListControl>
                <DataListDragButton aria-label={t("dragHelp")} />
              </DataListControl>
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    key={provider.alias}
                    data-testid={provider.alias}
                  >
                    {provider.alias}
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        ))}
      </DataList>
      <div className="pf-screen-reader" aria-live="assertive">
        {liveText}
      </div>
    </Modal>
  );
};
