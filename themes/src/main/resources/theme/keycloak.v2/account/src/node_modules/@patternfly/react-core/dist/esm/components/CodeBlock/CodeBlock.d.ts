import * as React from 'react';
export interface CodeBlockProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the code block */
    children?: React.ReactNode;
    /** Additional classes passed to the code block wrapper */
    className?: string;
    /** Actions in the code block header. Should be wrapped with CodeBlockAction. */
    actions?: React.ReactNode;
}
export declare const CodeBlock: React.FunctionComponent<CodeBlockProps>;
//# sourceMappingURL=CodeBlock.d.ts.map