import * as React from 'react';
export interface DataListContentProps extends React.HTMLProps<HTMLElement> {
    /** Content rendered inside the DataList item */
    children?: React.ReactNode;
    /** Additional classes added to the DataList cell */
    className?: string;
    /** Identify the DataListContent item */
    id?: string;
    /** Id for the row */
    rowid?: string;
    /** Flag to show if the expanded content of the DataList item is visible */
    isHidden?: boolean;
    /** Flag to remove padding from the expandable content */
    hasNoPadding?: boolean;
    /** Adds accessible text to the DataList toggle */
    'aria-label': string;
}
export declare const DataListContent: React.FunctionComponent<DataListContentProps>;
//# sourceMappingURL=DataListContent.d.ts.map