import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
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
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";

type ManageOrderDialogProps = {
  orgId?: string;
  hideRealmBasedIdps?: boolean;
  onClose: () => void;
};

const toDraggableItems = (aliases: string[]): DraggableObject[] =>
  aliases.map((alias) => ({
    id: alias,
    content: (
      <DataListItemCells
        dataListCells={[
          <DataListCell key={alias} data-testid={alias}>
            {alias}
          </DataListCell>,
        ]}
      />
    ),
  }));

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
  const [items, setItems] = useState<DraggableObject[]>([]);

  const order = items.map((item) => item.id as string);

  useFetch(
    () =>
      orgId
        ? adminClient.organizations.listIdentityProviders({ orgId })
        : adminClient.identityProviders.find({ realmOnly: hideRealmBasedIdps }),
    (providers) => {
      setProviders(providers);
      setItems(
        toDraggableItems(
          sortBy(providers, ["config.guiOrder", "alias"]).map(
            (provider) => provider.alias!,
          ),
        ),
      );
    },
    [],
  );

  if (!providers) {
    return <KeycloakSpinner />;
  }

  const title = t("manageDisplayOrder");

  return (
    <Modal
      variant={ModalVariant.small}
      isOpen
      onClose={onClose}
      aria-label={title}
    >
      <ModalHeader title={title} />
      <ModalBody>
        <Content className="pf-v6-u-pb-lg">
          <Content component="p">{t("orderDialogIntro")}</Content>
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
        </Button>
        <Button
          id="modal-cancel"
          data-testid="cancel"
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
