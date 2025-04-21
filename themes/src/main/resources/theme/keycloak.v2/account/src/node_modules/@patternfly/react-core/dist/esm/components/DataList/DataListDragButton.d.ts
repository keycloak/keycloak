import * as React from 'react';
export interface DataListDragButtonProps extends React.HTMLProps<HTMLButtonElement> {
    /** Additional classes added to the drag button */
    className?: string;
    /** Sets button type */
    type?: 'button' | 'submit' | 'reset';
    /** Flag indicating if drag is disabled for the item */
    isDisabled?: boolean;
}
export declare const DataListDragButton: React.FunctionComponent<DataListDragButtonProps>;
//# sourceMappingURL=DataListDragButton.d.ts.map