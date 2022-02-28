import * as React from 'react';
export interface TextContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered within the TextContent */
    children?: React.ReactNode;
    /** Additional classes added to the TextContent */
    className?: string;
}
export declare const TextContent: React.FunctionComponent<TextContentProps>;
