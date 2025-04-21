import * as React from 'react';
export declare enum TextListItemVariants {
    li = "li",
    dt = "dt",
    dd = "dd"
}
export interface TextListItemProps extends React.HTMLProps<HTMLElement> {
    /** Content rendered within the TextListItem */
    children?: React.ReactNode;
    /** Additional classes added to the TextListItem */
    className?: string;
    /** The text list item component */
    component?: 'li' | 'dt' | 'dd';
}
export declare const TextListItem: React.FunctionComponent<TextListItemProps>;
//# sourceMappingURL=TextListItem.d.ts.map