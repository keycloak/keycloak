import * as React from 'react';
export interface MultipleFileUploadStatusProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside multi file upload status list */
    children?: React.ReactNode;
    /** Class to add to outer div */
    className?: string;
    /** String to show in the status toggle */
    statusToggleText?: string;
    /** Icon to show in the status toggle */
    statusToggleIcon?: 'danger' | 'success' | 'inProgress' | React.ReactNode;
}
export declare const MultipleFileUploadStatus: React.FunctionComponent<MultipleFileUploadStatusProps>;
//# sourceMappingURL=MultipleFileUploadStatus.d.ts.map