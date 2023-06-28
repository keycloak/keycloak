import {
  DataListItem,
  DataListItemRow,
  DataListItemCells,
  DataListCell,
} from "@patternfly/react-core";

type EmptyRowProps = {
  message: string;
};

export const EmptyRow = ({ message }: EmptyRowProps) => {
  return (
    <DataListItem className="pf-u-align-items-center pf-p-b-0">
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[<DataListCell key="0">{message}</DataListCell>]}
        />
      </DataListItemRow>
    </DataListItem>
  );
};
