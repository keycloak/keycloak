import * as React from 'react';
export interface DataListCellProps extends Omit<React.HTMLProps<HTMLDivElement>, 'width'> {
    /** Content rendered inside the DataList cell */
    children?: React.ReactNode;
    /** Additional classes added to the DataList cell */
    className?: string;
    /** Width (from 1-5) to the DataList cell */
    width?: 1 | 2 | 3 | 4 | 5;
    /** Enables the body Content to fill the height of the card */
    isFilled?: boolean;
    /**  Aligns the cell content to the right of its parent. */
    alignRight?: boolean;
    /** Set to true if the cell content is an Icon */
    isIcon?: boolean;
}
export declare const DataListCell: React.FunctionComponent<DataListCellProps>;
