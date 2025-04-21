import * as React from 'react';
export interface DataListItemProps extends Omit<React.HTMLProps<HTMLLIElement>, 'children' | 'ref'> {
    /** Flag to show if the expanded content of the DataList item is visible */
    isExpanded?: boolean;
    /** Content rendered inside the DataList item */
    children: React.ReactNode;
    /** Additional classes added to the DataList item should be either <DataListItemRow> or <DataListContent> */
    className?: string;
    /** Adds accessible text to the DataList item */
    'aria-labelledby': string;
    /** Unique id for the DataList item */
    id?: string;
    /** @beta Aria label to apply to the selectable input if one is rendered */
    selectableInputAriaLabel?: string;
}
export interface DataListItemChildProps {
    /** Id for the row */
    rowid: string;
}
export declare class DataListItem extends React.Component<DataListItemProps> {
    static displayName: string;
    static defaultProps: DataListItemProps;
    render(): JSX.Element;
}
//# sourceMappingURL=DataListItem.d.ts.map