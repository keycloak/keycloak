import * as React from 'react';
export interface MultipleFileUploadTitleProps extends React.HTMLProps<HTMLDivElement> {
    /** Class to add to outer div */
    className?: string;
    /** Content rendered inside the title icon div */
    icon?: React.ReactNode;
    /** Content rendered inside the title text div */
    text?: React.ReactNode;
    /** Content rendered inside the title text separator div */
    textSeparator?: React.ReactNode;
}
export declare const MultipleFileUploadTitle: React.FunctionComponent<MultipleFileUploadTitleProps>;
//# sourceMappingURL=MultipleFileUploadTitle.d.ts.map