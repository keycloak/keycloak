import {
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";

type AddProviderDialogProps = {
  onConfirm: (providerId: string) => void;
  toggleDialog: () => void;
};

export const AddProviderDialog = ({
  onConfirm,
  toggleDialog,
}: AddProviderDialogProps) => {
  const { t } = useTranslation();
  const serverInfo = useServerInfo();
  const providers = Object.keys(
    serverInfo.providers?.["client-registration-policy"].providers || [],
  );

  const descriptions =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy"
    ];
  const localeSort = useLocaleSort();

  const rows = useMemo(
    () =>
      localeSort(
        descriptions?.filter((d) => providers.includes(d.id)) || [],
        mapByKey("id"),
      ),
    [providers, descriptions],
  );
  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("chooseAPolicyProvider")}
      isOpen
      onClose={toggleDialog}
    >
      <DataList
        onSelectDataListItem={(_event, id) => {
          onConfirm(id);
          toggleDialog();
        }}
        aria-label={t("addPredefinedMappers")}
        isCompact
      >
        <DataListItem aria-label={t("headerName")} id="header">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[t("name"), t("description")].map((name) => (
                <DataListCell style={{ fontWeight: 700 }} key={name}>
                  {name}
                </DataListCell>
              ))}
            />
          </DataListItemRow>
        </DataListItem>
        {rows.map((provider) => (
          <DataListItem
            aria-label={provider.id}
            key={provider.id}
            data-testid={provider.id}
            id={provider.id}
          >
            <DataListItemRow>
              <DataListItemCells
                dataListCells={[
                  <DataListCell width={2} key={`name-${provider.id}`}>
                    {provider.id}
                  </DataListCell>,
                  <DataListCell width={4} key={`description-${provider.id}`}>
                    {provider.helpText}
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        ))}
      </DataList>
    </Modal>
  );
};
