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

type ManageOrderDialogProps = {
  providers: IdentityProviderRepresentation[];
  onClose: () => void;
};

export const ManageOrderDialog = ({
  providers,
  onClose,
}: ManageOrderDialogProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [alias, setAlias] = useState("");
  const [liveText, setLiveText] = useState("");
  const [order, setOrder] = useState(
    providers.map((provider) => provider.alias!),
  );

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

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("manageDisplayOrder")}
      isOpen={true}
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
