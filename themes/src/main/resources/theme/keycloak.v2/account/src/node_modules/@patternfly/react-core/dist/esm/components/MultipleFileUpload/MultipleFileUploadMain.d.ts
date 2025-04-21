import * as React from 'react';
export interface MultipleFileUploadMainProps extends React.HTMLProps<HTMLDivElement> {
    /** Class to add to outer div */
    className?: string;
    /** Content rendered inside the title icon div */
    titleIcon?: React.ReactNode;
    /** Content rendered inside the title text div */
    titleText?: React.ReactNode;
    /** Content rendered inside the title text separator div */
    titleTextSeparator?: React.ReactNode;
    /** Content rendered inside the info div */
    infoText?: React.ReactNode;
    /** Flag to prevent the upload button from being rendered */
    isUploadButtonHidden?: boolean;
}
export declare const MultipleFileUploadMain: React.FunctionComponent<MultipleFileUploadMainProps>;
//# sourceMappingURL=MultipleFileUploadMain.d.ts.map