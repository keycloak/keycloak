import * as React from 'react';
export interface MastheadContentProps extends React.DetailedHTMLProps<React.HTMLProps<HTMLDivElement>, HTMLDivElement> {
    /** Content rendered inside of the masthead content block. */
    children?: React.ReactNode;
    /** Additional classes added to the masthead content. */
    className?: string;
}
export declare const MastheadContent: React.FunctionComponent<MastheadContentProps>;
//# sourceMappingURL=MastheadContent.d.ts.map