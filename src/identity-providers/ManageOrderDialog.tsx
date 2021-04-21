import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import _ from "lodash";
import {
  AlertVariant,
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
  TextContent,
  Text,
} from "@patternfly/react-core";
import IdentityProviderRepresentation from "keycloak-admin/lib/defs/identityProviderRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";

type ManageOderDialogProps = {
  providers: IdentityProviderRepresentation[];
  onClose: () => void;
};

export const ManageOderDialog = ({
  providers,
  onClose,
}: ManageOderDialogProps) => {
  const { t } = useTranslation("identity-providers");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const [alias, setAlias] = useState("");
  const [liveText, setLiveText] = useState("");
  const [order, setOrder] = useState(
    providers.map((provider) => provider.alias!)
  );

  const onDragStart = (id: string) => {
    setAlias(id);
    setLiveText(t("onDragStart", { id }));
  };

  const onDragMove = () => {
    setLiveText(t("onDragMove", { alias }));
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
          key="confirm"
          onClick={() => {
            order.map(async (alias, index) => {
              const provider = providers.find((p) => p.alias === alias)!;
              provider.config!.guiOrder = index;
              try {
                await adminClient.identityProviders.update({ alias }, provider);
                addAlert(t("orderChangeSuccess"), AlertVariant.success);
              } catch (error) {
                addAlert(t("orderChangeError", { error }), AlertVariant.danger);
              }
            });

            onClose();
          }}
        >
          {t("common:save")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <TextContent className="pf-u-pb-lg">
        <Text>{t("oderDialogIntro")}</Text>
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
        {_.sortBy(providers, "config.guiOrder").map((provider) => (
          <DataListItem
            aria-labelledby={provider.alias}
            id={provider.alias}
            key={provider.alias}
          >
            <DataListItemRow>
              <DataListControl>
                <DataListDragButton
                  aria-label="Reorder"
                  aria-labelledby={provider.alias}
                  aria-describedby={t("manageOrderItemAria")}
                  aria-pressed="false"
                />
              </DataListControl>
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    key={`${provider.alias}-cell`}
                    data-testid={provider.alias}
                  >
                    <span id={provider.alias}>{provider.alias}</span>
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
