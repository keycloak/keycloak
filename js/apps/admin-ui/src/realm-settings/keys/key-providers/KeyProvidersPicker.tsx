import {
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { KEY_PROVIDER_TYPE } from "../../../util";

type KeyProvidersPickerProps = {
  onConfirm: (provider: string) => void;
  onClose: () => void;
};

export const KeyProvidersPicker = ({
  onConfirm,
  onClose,
}: KeyProvidersPickerProps) => {
  const { t } = useTranslation();
  const serverInfo = useServerInfo();
  const keyProviderComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];
  return (
    <Modal variant="medium" title={t("addProvider")} isOpen onClose={onClose}>
      <DataList
        onSelectDataListItem={(_event, id) => {
          onConfirm(id);
        }}
        aria-label={t("addPredefinedMappers")}
        isCompact
      >
        {keyProviderComponentTypes.map((provider) => (
          <DataListItem
            aria-label={provider.id}
            key={provider.id}
            id={provider.id}
          >
            <DataListItemRow>
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    key={`name-${provider.id}`}
                    data-testid={`option-${provider.id}`}
                  >
                    {provider.id}
                  </DataListCell>,
                  <DataListCell width={2} key={`helpText-${provider.helpText}`}>
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
