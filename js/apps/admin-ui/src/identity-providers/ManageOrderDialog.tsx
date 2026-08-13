import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
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

type ManageOrderDialogProps = {
  orgId?: string;
  hideRealmBasedIdps?: boolean;
  onClose: () => void;
};

export const ManageOrderDialog = ({
  orgId,
  hideRealmBasedIdps = false,
  onClose,
}: ManageOrderDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [liveText, setLiveText] = useState("");
  const [providers, setProviders] =
    useState<IdentityProviderRepresentation[]>();
  const [order, setOrder] = useState<string[]>([]);

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

  useFetch(
    () =>
      orgId
        ? adminClient.organizations.listIdentityProviders({ orgId })
        : adminClient.identityProviders.find({ realmOnly: hideRealmBasedIdps }),
    (providers) => {
      setProviders(providers);
      setOrder(
        sortBy(providers, ["config.guiOrder", "alias"]).map(
          (provider) => provider.alias!,
        ),
      );
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
      <TextContent className="pf-v5-u-pb-lg">
        <Text>{t("orderDialogIntro")}</Text>
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
            {order.map((alias) => (
              <Draggable hasNoWrapper key={alias}>
                <DataListItem aria-label={alias} id={alias}>
                  <DataListItemRow>
                    <DataListControl>
                      <DataListDragButton aria-label={t("dragHelp")} />
                    </DataListControl>
                    <DataListItemCells
                      dataListCells={[
                        <DataListCell key={alias} data-testid={alias}>
                          {alias}
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
