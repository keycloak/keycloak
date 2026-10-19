import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  Button,
  ButtonVariant,
  Content,
  DataList,
  DataListCell,
  DataListItemCells,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
} from "@patternfly/react-core";
import { DragDropSort, DraggableObject } from "@patternfly/react-drag-drop";
import { sortBy } from "lodash-es";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

type ManagePriorityDialogProps = {
  components: ComponentRepresentation[];
  onClose: () => void;
};

const toDraggableItems = (names: string[]): DraggableObject[] =>
  names.map((name) => ({
    id: name,
    content: (
      <DataListItemCells
        dataListCells={[
          <DataListCell key={name} data-testid={name}>
            {name}
          </DataListCell>,
        ]}
      />
    ),
  }));

export const ManagePriorityDialog = ({
  components,
  onClose,
}: ManagePriorityDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [liveText, setLiveText] = useState("");
  const initialOrder = useMemo(
    () =>
      sortBy(components, "config.priority", "name").map(
        (component) => component.name!,
      ),
    [components],
  );
  const [items, setItems] = useState(() => toDraggableItems(initialOrder));
  const order = items.map((item) => item.id as string);

  const title = t("managePriorityOrder");

  return (
    <Modal
      variant={ModalVariant.small}
      isOpen={true}
      onClose={onClose}
      aria-label={title}
    >
      <ModalHeader title={title} />
      <ModalBody>
        <Content className="pf-v6-u-pb-lg">
          <Content component="p">{t("managePriorityInfo")}</Content>
        </Content>

        <DragDropSort
          items={items}
          variant="DataList"
          overlayProps={{ isCompact: true }}
          onDrag={(_, index) => {
            setLiveText(t("onDragStart", { item: order[index] }));
          }}
          onDrop={(_, newItems) => {
            setItems(newItems);
            setLiveText(
              t("onDragFinish", { list: newItems.map((item) => item.id) }),
            );
          }}
        >
          <DataList
            aria-label={t("manageOrderTableAria")}
            data-testid="manageOrderDataList"
            isCompact
          />
        </DragDropSort>
        <div className="pf-v6-screen-reader" aria-live="assertive">
          {liveText}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          id="modal-confirm"
          key="confirm"
          onClick={async () => {
            const updates = order.map((name, index) => {
              const component = components.find((c) => c.name === name)!;
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
        </Button>
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>
      </ModalFooter>
    </Modal>
  );
};
