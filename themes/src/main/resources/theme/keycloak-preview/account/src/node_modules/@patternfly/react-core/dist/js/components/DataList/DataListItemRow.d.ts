import * as React from 'react';
export interface DataListItemRowProps extends Omit<React.HTMLProps<HTMLDivElement>, 'children'> {
    children: React.ReactNode;
    className?: string;
    rowid?: string;
}
export declare const DataListItemRow: React.FunctionComponent<DataListItemRowProps>;
