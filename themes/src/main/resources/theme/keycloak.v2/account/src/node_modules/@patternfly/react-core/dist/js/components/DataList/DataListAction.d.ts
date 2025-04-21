import * as React from 'react';
export interface DataListActionProps extends Omit<React.HTMLProps<HTMLDivElement>, 'children'> {
    /** Content rendered as DataList Action  (e.g <Button> or <Dropdown>) */
    children: React.ReactNode;
    /** Additional classes added to the DataList Action */
    className?: string;
    /** Identify the DataList toggle number */
    id: string;
    /** Adds accessible text to the DataList Action */
    'aria-labelledby': string;
    /** Adds accessible text to the DataList Action */
    'aria-label': string;
    /** What breakpoints to hide/show the data list action */
    visibility?: {
        default?: 'hidden' | 'visible';
        sm?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** Flag to indicate that the action is a plain button (e.g. kebab dropdown toggle) so that styling is applied to align the button */
    isPlainButtonAction?: boolean;
}
export declare const DataListAction: React.FunctionComponent<DataListActionProps>;
//# sourceMappingURL=DataListAction.d.ts.map