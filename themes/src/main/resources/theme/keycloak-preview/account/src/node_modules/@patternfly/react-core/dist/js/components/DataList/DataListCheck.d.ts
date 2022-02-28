import * as React from 'react';
export interface DataListCheckProps extends Omit<React.HTMLProps<HTMLInputElement>, 'onChange' | 'checked'> {
    /** Additional classes added to the DataList item checkbox */
    className?: string;
    /** Flag to show if the DataList checkbox selection is valid or invalid */
    isValid?: boolean;
    /** Flag to show if the DataList checkbox is disabled */
    isDisabled?: boolean;
    /** Flag to show if the DataList checkbox is checked */
    isChecked?: boolean;
    /** Alternate Flag to show if the DataList checkbox is checked */
    checked?: boolean;
    /** A callback for when the DataList checkbox selection changes */
    onChange?: (checked: boolean, event: React.FormEvent<HTMLInputElement>) => void;
    /** Aria-labelledby of the DataList checkbox */
    'aria-labelledby': string;
}
export declare const DataListCheck: React.FunctionComponent<DataListCheckProps>;
