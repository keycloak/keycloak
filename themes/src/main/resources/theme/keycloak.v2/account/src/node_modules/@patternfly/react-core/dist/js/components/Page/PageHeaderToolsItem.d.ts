import * as React from 'react';
export interface PageHeaderToolsItemProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered in page header tools item. */
    children: React.ReactNode;
    /** Additional classes added to the page header tools item. */
    className?: string;
    /** HTML id of the PageHeaderToolsItem */
    id?: string;
    /** Visibility at various breakpoints. */
    visibility?: {
        default?: 'hidden' | 'visible';
        sm?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** True to make an icon button appear selected */
    isSelected?: boolean;
}
export declare const PageHeaderToolsItem: React.FunctionComponent<PageHeaderToolsItemProps>;
//# sourceMappingURL=PageHeaderToolsItem.d.ts.map