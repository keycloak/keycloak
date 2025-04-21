import * as React from 'react';
export interface TextContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered within the TextContent */
    children?: React.ReactNode;
    /** Additional classes added to the TextContent */
    className?: string;
    /** Flag to indicate the all links in a the content block have visited styles applied if the browser determines the link has been visited */
    isVisited?: boolean;
}
export declare const TextContent: React.FunctionComponent<TextContentProps>;
//# sourceMappingURL=TextContent.d.ts.map