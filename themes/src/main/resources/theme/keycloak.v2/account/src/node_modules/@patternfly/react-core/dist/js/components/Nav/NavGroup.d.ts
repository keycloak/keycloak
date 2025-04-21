import * as React from 'react';
export interface NavGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Title shown for the group */
    title?: string;
    /** Anything that can be rendered inside of the group */
    children?: React.ReactNode;
    /** Additional classes added to the container */
    className?: string;
    /** Identifier to use for the section aria label */
    id?: string;
}
export declare const NavGroup: React.FunctionComponent<NavGroupProps>;
//# sourceMappingURL=NavGroup.d.ts.map