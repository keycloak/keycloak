import * as React from 'react';
export interface DataListToggleProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the DataList cell */
    className?: string;
    /** Flag to show if the expanded content of the DataList item is visible */
    isExpanded?: boolean;
    /** Identify the DataList toggle number */
    id: string;
    /** Id for the row */
    rowid?: string;
    /** Adds accessible text to the DataList toggle */
    'aria-labelledby'?: string;
    /** Adds accessible text to the DataList toggle */
    'aria-label'?: string;
    /** Allows users of some screen readers to shift focus to the controlled element. Should be used when the controlled contents are not adjacent to the toggle that controls them. */
    'aria-controls'?: string;
}
export declare const DataListToggle: React.FunctionComponent<DataListToggleProps>;
//# sourceMappingURL=DataListToggle.d.ts.map