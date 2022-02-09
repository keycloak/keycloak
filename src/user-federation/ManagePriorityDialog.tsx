import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { sortBy } from "lodash-es";
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
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";

type ManagePriorityDialogProps = {
  components: ComponentRepresentation[];
  onClose: () => void;
};

export const ManagePriorityDialog = ({
  components,
  onClose,
}: ManagePriorityDialogProps) => {
  const { t } = useTranslation("user-federation");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [id, setId] = useState("");
  const [liveText, setLiveText] = useState("");
  const [order, setOrder] = useState(
    components.map((component) => component.name!)
  );

  const onDragStart = (id: string) => {
    setId(id);
    setLiveText(t("common:onDragStart", { item: id }));
  };

  const onDragMove = () => {
    setLiveText(t("common:onDragMove", { item: id }));
  };

  const onDragCancel = () => {
    setLiveText(t("common:onDragCancel"));
  };

  const onDragFinish = (providerOrder: string[]) => {
    setLiveText(t("common:onDragFinish", { list: providerOrder }));
    setOrder(providerOrder);
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("managePriorityOrder")}
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          onClick={() => {
            order.map(async (name, index) => {
              const component = components!.find((c) => c.name === name)!;
              component.config!.priority = [index.toString()];
              try {
                const id = component.id!;
                await adminClient.components.update({ id }, component);
                addAlert(t("orderChangeSuccess"), AlertVariant.success);
              } catch (error) {
                addError("orderChangeError", error);
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
        <Text>{t("managePriorityInfo")}</Text>
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
        {sortBy(components, "config.priority").map((component) => (
          <DataListItem
            aria-labelledby={component.name}
            id={component.name}
            key={component.name}
          >
            <DataListItemRow>
              <DataListControl>
                <DataListDragButton
                  aria-label="Reorder"
                  aria-labelledby={component.name}
                  aria-describedby={t("manageOrderItemAria")}
                  aria-pressed="false"
                />
              </DataListControl>
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    key={`${component.name}-cell`}
                    data-testid={component.name}
                  >
                    <span id={component.name}>{component.name}</span>
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
