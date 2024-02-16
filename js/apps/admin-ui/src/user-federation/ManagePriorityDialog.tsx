import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
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

type ManagePriorityDialogProps = {
  components: ComponentRepresentation[];
  onClose: () => void;
};

export const ManagePriorityDialog = ({
  components,
  onClose,
}: ManagePriorityDialogProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [id, setId] = useState("");
  const [liveText, setLiveText] = useState("");
  const [order, setOrder] = useState(
    components.map((component) => component.name!),
  );

  const onDragStart = (id: string) => {
    setId(id);
    setLiveText(t("onDragStart", { item: id }));
  };

  const onDragMove = () => {
    setLiveText(t("onDragMove", { item: id }));
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
      title={t("managePriorityOrder")}
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          onClick={async () => {
            const updates = order.map((name, index) => {
              const component = components!.find((c) => c.name === name)!;
              component.config!.priority = [index.toString()];
              return adminClient.components.update(
                { id: component.id! },
                component,
              );
            });

            try {
              await Promise.all(updates);
              addAlert(t("orderChangeSuccessUserFed"));
            } catch (error) {
              addError("orderChangeErrorUserFed", error);
            }

            onClose();
          }}
        >
          {t("save")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
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
        {sortBy(components, "config.priority", "name").map((component) => (
          <DataListItem
            aria-label={component.name}
            id={component.name}
            key={component.name}
          >
            <DataListItemRow>
              <DataListControl>
                <DataListDragButton aria-label={t("dragHelp")} />
              </DataListControl>
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    key={component.name}
                    data-testid={component.name}
                  >
                    {component.name}
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
