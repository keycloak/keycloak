import { useTranslation } from "react-i18next";
import { Button, ToolbarItem } from "@patternfly/react-core";

import {
  Action,
  KeycloakDataTable,
} from "../../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";

export type EventType = {
  id: string;
};

type EventsTypeTableProps = {
  ariaLabelKey?: string;
  eventTypes: string[];
  addTypes?: () => void;
  onSelect?: (value: EventType[]) => void;
  onDelete?: (value: EventType) => void;
};

export function EventsTypeTable({
  ariaLabelKey = "userEventsRegistered",
  eventTypes,
  addTypes,
  onSelect,
  onDelete,
}: EventsTypeTableProps) {
  const { t } = useTranslation();

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
      onSelect={onSelect ? onSelect : undefined}
      canSelectAll={!!onSelect}
      toolbarItem={
        addTypes && (
          <ToolbarItem>
            <Button id="addTypes" onClick={addTypes} data-testid="addTypes">
              {t("addSavedTypes")}
            </Button>
          </ToolbarItem>
        )
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
          displayKey: "description",
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
