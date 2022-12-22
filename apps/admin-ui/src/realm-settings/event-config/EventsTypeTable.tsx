import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { Button, ToolbarItem } from "@patternfly/react-core";
import type { IFormatterValueType } from "@patternfly/react-table";

import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";

export type EventType = {
  id: string;
};

type EventsTypeTableProps = {
  loader: () => Promise<EventType[]>;
  addTypes?: () => void;
  onSelect?: (value: EventType[]) => void;
  onDelete?: (value: EventType) => void;
};

export function EventsTypeTable({
  loader,
  addTypes,
  onSelect,
  onDelete,
}: EventsTypeTableProps) {
  const { t } = useTranslation("realm-settings");

  const DescriptionCell = (event: EventType) => (
    <Fragment key={event.id}>
      {t(`eventTypes.${event.id}.description`)}
    </Fragment>
  );

  return (
    <KeycloakDataTable
      ariaLabelKey="userEventsRegistered"
      searchPlaceholderKey="realm-settings:searchEventType"
      loader={loader}
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
                title: t("common:remove"),
                onRowClick: onDelete,
              },
            ]
      }
      columns={[
        {
          name: "id",
          displayKey: "realm-settings:eventType",
          cellFormatters: [
            (data?: IFormatterValueType) => t(`eventTypes.${data}.name`),
          ],
        },
        {
          name: "description",
          displayKey: "description",
          cellRenderer: DescriptionCell,
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
