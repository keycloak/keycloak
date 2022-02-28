import * as React from 'react';
export interface DataListProps extends React.HTMLProps<HTMLUListElement> {
    children?: React.ReactNode;
    className?: string;
    'aria-label': string;
    onSelectDataListItem?: (id: string) => void;
    selectedDataListItemId?: string;
    /** Flag indicating if DataList should have compact styling */
    isCompact?: boolean;
}
interface DataListContextProps {
    isSelectable: boolean;
    selectedDataListItemId: string;
    updateSelectedDataListItem: (id: string) => void;
}
export declare const DataListContext: React.Context<Partial<DataListContextProps>>;
export declare const DataList: React.FunctionComponent<DataListProps>;
export {};
