import * as React from 'react';
export interface SimpleListGroupProps extends Omit<React.HTMLProps<HTMLTableSectionElement>, 'title'> {
    /** Content rendered inside the SimpleList group */
    children?: React.ReactNode;
    /** Additional classes added to the SimpleList <ul> */
    className?: string;
    /** Additional classes added to the SimpleList group title */
    titleClassName?: string;
    /** Title of the SimpleList group */
    title?: React.ReactNode;
    /** ID of SimpleList group */
    id?: string;
}
export declare const SimpleListGroup: React.FunctionComponent<SimpleListGroupProps>;
//# sourceMappingURL=SimpleListGroup.d.ts.map