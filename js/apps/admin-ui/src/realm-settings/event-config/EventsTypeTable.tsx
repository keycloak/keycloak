import {
  Action,
  KeycloakDataTable,
  ListEmptyState,
} from "@keycloak/keycloak-ui-shared";
import { Button, ToolbarItem } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { translationFormatter } from "../../utils/translationFormatter";

export type EventType = {
  id: string;
};

type EventsTypeTableProps = {
  ariaLabelKey?: string;
  eventTypes: string[];
  addTypes?: () => void;
  onSelect?: (value: EventType[]) => void;
  onDelete?: (value: EventType) => void;
  onDeleteAll?: (value: EventType[]) => void;
};

export function EventsTypeTable({
  ariaLabelKey = "userEventsRegistered",
  eventTypes,
  addTypes,
  onSelect,
  onDelete,
  onDeleteAll,
}: EventsTypeTableProps) {
  const { t } = useTranslation();
  const [selectedTypes, setSelectedTypes] = useState<EventType[]>([]);

  const data = eventTypes.map((type) => ({
    id: type,
    name: t(`eventTypes.${type}.name`),
    description: t(`eventTypes.${type}.description`),
  }));
  return (
    <KeycloakDataTable
      ariaLabelKey={ariaLabelKey}
      searchPlaceholderKey="searchEventType"
      loader={data}
      onSelect={onSelect ? onSelect : setSelectedTypes}
      canSelectAll
      toolbarItem={
        <>
          {addTypes && (
            <ToolbarItem>
              <Button id="addTypes" onClick={addTypes} data-testid="addTypes">
                {t("addSavedTypes")}
              </Button>
            </ToolbarItem>
          )}
          {onDeleteAll && (
            <ToolbarItem>
              <Button
                onClick={() => onDeleteAll(selectedTypes)}
                data-testid="removeAll"
                variant="secondary"
                isDisabled={selectedTypes.length === 0}
              >
                {t("remove")}
              </Button>
            </ToolbarItem>
          )}
        </>
      }
      actions={
        !onDelete
          ? []
          : [
              {
                title: t("remove"),
                onRowClick: onDelete,
              } as Action<EventType>,
            ]
      }
      columns={[
        {
          name: "name",
          displayKey: "eventType",
        },
        {
          name: "description",
          cellFormatters: [translationFormatter(t)],
        },
      ]}
      emptyState={
        <ListEmptyState
          message={t("emptyEvents")}
          instructions={t("emptyEventsInstructions")}
        />
      }
    />
  );
}
