import * as React from 'react';
export interface SplitItemProps extends React.HTMLProps<HTMLDivElement> {
    /** Flag indicating if this Split Layout item should fill the available horizontal space. */
    isFilled?: boolean;
    /** content rendered inside the Split Layout Item */
    children?: React.ReactNode;
    /** additional classes added to the Split Layout Item */
    className?: string;
}
export declare const SplitItem: React.FunctionComponent<SplitItemProps>;
//# sourceMappingURL=SplitItem.d.ts.map