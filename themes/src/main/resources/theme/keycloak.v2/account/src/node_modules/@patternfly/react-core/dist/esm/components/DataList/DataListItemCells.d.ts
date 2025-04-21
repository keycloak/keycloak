import * as React from 'react';
export interface DataListItemCellsProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the DataList item Content Wrapper.  Children should be one ore more <DataListCell> nodes */
    className?: string;
    /** Array of <DataListCell> nodes that are rendered one after the other. */
    dataListCells?: React.ReactNode;
    /** Id for the row */
    rowid?: string;
}
export declare const DataListItemCells: React.FunctionComponent<DataListItemCellsProps>;
//# sourceMappingURL=DataListItemCells.d.ts.map