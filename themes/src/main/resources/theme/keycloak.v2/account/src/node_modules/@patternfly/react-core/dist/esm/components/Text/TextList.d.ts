import * as React from 'react';
export declare enum TextListVariants {
    ul = "ul",
    ol = "ol",
    dl = "dl"
}
export interface TextListProps extends React.HTMLProps<HTMLElement> {
    /** Content rendered within the TextList */
    children?: React.ReactNode;
    /** Additional classes added to the TextList */
    className?: string;
    /** The text list component */
    component?: 'ul' | 'ol' | 'dl';
}
export declare const TextList: React.FunctionComponent<TextListProps>;
//# sourceMappingURL=TextList.d.ts.map