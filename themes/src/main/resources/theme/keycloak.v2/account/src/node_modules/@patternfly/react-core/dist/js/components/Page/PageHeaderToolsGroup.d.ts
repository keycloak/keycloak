import * as React from 'react';
export interface PageHeaderToolsGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered in the page header tools group */
    children: React.ReactNode;
    /** Additional classes added to the page header tools group. */
    className?: string;
    /** Visibility at various breakpoints. */
    visibility?: {
        default?: 'hidden' | 'visible';
        sm?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
}
export declare const PageHeaderToolsGroup: React.FunctionComponent<PageHeaderToolsGroupProps>;
//# sourceMappingURL=PageHeaderToolsGroup.d.ts.map