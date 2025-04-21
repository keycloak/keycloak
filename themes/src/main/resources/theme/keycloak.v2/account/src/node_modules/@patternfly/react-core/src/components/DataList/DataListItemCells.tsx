import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';

export interface DataListItemCellsProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the DataList item Content Wrapper.  Children should be one ore more <DataListCell> nodes */
  className?: string;
  /** Array of <DataListCell> nodes that are rendered one after the other. */
  dataListCells?: React.ReactNode;
  /** Id for the row */
  rowid?: string;
}

export const DataListItemCells: React.FunctionComponent<DataListItemCellsProps> = ({
  className = '',
  dataListCells,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  rowid = '',
  ...props
}: DataListItemCellsProps) => (
  <div className={css(styles.dataListItemContent, className)} {...props}>
    {dataListCells}
  </div>
);
DataListItemCells.displayName = 'DataListItemCells';
