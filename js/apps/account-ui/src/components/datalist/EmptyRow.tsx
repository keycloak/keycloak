import {
  DataListItem,
  DataListItemRow,
  DataListItemCells,
  DataListCell,
} from "@patternfly/react-core";

type EmptyRowProps = {
  message: string;
};

export const EmptyRow = ({ message, ...props }: EmptyRowProps) => {
  return (
    <DataListItem className="pf-v5-u-align-items-center pf-p-b-0">
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell key="0" {...props}>
              {message}
            </DataListCell>,
          ]}
        />
      </DataListItemRow>
    </DataListItem>
  );
};
