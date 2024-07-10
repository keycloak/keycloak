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
  DragDrop,
  Draggable,
  DraggableItemPosition,
  Droppable,
  Modal,
  ModalVariant,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { sortBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

type ManagePriorityDialogProps = {
  components: ComponentRepresentation[];
  onClose: () => void;
};

export const ManagePriorityDialog = ({
  components,
  onClose,
}: ManagePriorityDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [liveText, setLiveText] = useState("");
  const [order, setOrder] = useState(
    sortBy(components, "config.priority", "name").map(
      (component) => component.name!,
    ),
  );

  const onDragStart = ({ index }: DraggableItemPosition) => {
    setLiveText(t("onDragStart", { item: order[index] }));
    return true;
  };

  const onDragMove = ({ index }: DraggableItemPosition) => {
    setLiveText(t("onDragMove", { item: order[index] }));
  };

  const onDragFinish = (
    source: DraggableItemPosition,
    dest?: DraggableItemPosition,
  ) => {
    if (dest) {
      const result = [...order];
      const [removed] = result.splice(source.index, 1);
      result.splice(dest.index, 0, removed);
      setLiveText(t("onDragFinish", { list: result }));
      setOrder(result);
      return true;
    } else {
      setLiveText(t("onDragCancel"));
      return false;
    }
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
      <TextContent className="pf-v5-u-pb-lg">
        <Text>{t("managePriorityInfo")}</Text>
      </TextContent>

      <DragDrop
        onDrag={onDragStart}
        onDragMove={onDragMove}
        onDrop={onDragFinish}
      >
        <Droppable hasNoWrapper>
          <DataList
            aria-label={t("manageOrderTableAria")}
            data-testid="manageOrderDataList"
            isCompact
          >
            {order.map((name) => (
              <Draggable key={name} hasNoWrapper>
                <DataListItem aria-label={name} id={name}>
                  <DataListItemRow>
                    <DataListControl>
                      <DataListDragButton aria-label={t("dragHelp")} />
                    </DataListControl>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key={name} data-testid={name}>
                          {name}
                        </DataListCell>,
                      ]}
                    />
                  </DataListItemRow>
                </DataListItem>
              </Draggable>
            ))}
          </DataList>
        </Droppable>
      </DragDrop>
      <div className="pf-v5-screen-reader" aria-live="assertive">
        {liveText}
      </div>
    </Modal>
  );
};
