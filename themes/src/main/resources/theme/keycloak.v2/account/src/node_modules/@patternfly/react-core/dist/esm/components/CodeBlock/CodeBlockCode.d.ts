import * as React from 'react';
export interface CodeBlockCodeProps extends React.HTMLProps<HTMLPreElement> {
    /** Code rendered inside the code block */
    children?: React.ReactNode;
    /** Additional classes passed to the code block pre wrapper */
    className?: string;
    /** Additional classes passed to the code block code */
    codeClassName?: string;
}
export declare const CodeBlockCode: React.FunctionComponent<CodeBlockCodeProps>;
//# sourceMappingURL=CodeBlockCode.d.ts.map