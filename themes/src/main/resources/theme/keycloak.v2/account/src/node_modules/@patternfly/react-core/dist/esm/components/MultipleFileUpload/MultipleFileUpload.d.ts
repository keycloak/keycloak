import * as React from 'react';
import { DropzoneProps } from 'react-dropzone';
export interface MultipleFileUploadProps extends Omit<React.HTMLProps<HTMLDivElement>, 'value'> {
    /** Content rendered inside the multi upload field */
    children?: React.ReactNode;
    /** Class to add to outer div */
    className?: string;
    /** Optional extra props to customize react-dropzone. */
    dropzoneProps?: DropzoneProps;
    /** Flag setting the component to horizontal styling mode */
    isHorizontal?: boolean;
    /** When files are dropped or uploaded this callback will be called with all accepted files */
    onFileDrop?: (data: File[]) => void;
}
export declare const MultipleFileUploadContext: React.Context<{
    open: () => void;
}>;
export declare const MultipleFileUpload: React.FunctionComponent<MultipleFileUploadProps>;
//# sourceMappingURL=MultipleFileUpload.d.ts.map