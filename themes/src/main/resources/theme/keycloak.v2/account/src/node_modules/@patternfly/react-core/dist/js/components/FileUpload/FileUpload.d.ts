import * as React from 'react';
import { DropzoneProps } from 'react-dropzone';
import { FileUploadFieldProps } from './FileUploadField';
export interface FileUploadProps extends Omit<FileUploadFieldProps, 'children' | 'onBrowseButtonClick' | 'onClearButtonClick' | 'isDragActive' | 'containerRef'> {
    /** Unique id for the TextArea, also used to generate ids for accessible labels. */
    id: string;
    /** What type of file. Determines what is is passed to `onChange` and expected by `value`
     * (a string for 'text' and 'dataURL', or a File object otherwise. */
    type?: 'text' | 'dataURL';
    /** Value of the file's contents
     * (string if text file, File object otherwise) */
    value?: string | File;
    /** Value to be shown in the read-only filename field. */
    filename?: string;
    /** @deprecated A callback for when the file contents change. Please instead use onFileInputChange, onTextChange, onDataChange, onClearClick individually.  */
    onChange?: (value: string | File, filename: string, event: React.MouseEvent<HTMLButtonElement, MouseEvent> | React.DragEvent<HTMLElement> | React.ChangeEvent<HTMLElement>) => void;
    /** Change event emitted from the hidden \<input type="file" \> field associated with the component  */
    onFileInputChange?: (event: React.ChangeEvent<HTMLInputElement> | React.DragEvent<HTMLElement>, file: File) => void;
    /** Callback for clicking on the FileUploadField text area. By default, prevents a click in the text area from opening file dialog. */
    onClick?: (event: React.MouseEvent) => void;
    /** Additional classes added to the FileUpload container element. */
    className?: string;
    /** Flag to show if the field is disabled. */
    isDisabled?: boolean;
    /** Flag to show if the field is read only. */
    isReadOnly?: boolean;
    /** Flag to show if a file is being loaded. */
    isLoading?: boolean;
    /** Aria-valuetext for the loading spinner */
    spinnerAriaValueText?: string;
    /** Flag to show if the field is required. */
    isRequired?: boolean;
    /** Value to indicate if the field is modified to show that validation state.
     * If set to success, field will be modified to indicate valid state.
     * If set to error,  field will be modified to indicate error state.
     */
    validated?: 'success' | 'error' | 'default';
    /** Aria-label for the TextArea. */
    'aria-label'?: string;
    /** Placeholder string to display in the empty filename field */
    filenamePlaceholder?: string;
    /** Aria-label for the read-only filename field */
    filenameAriaLabel?: string;
    /** Text for the Browse button */
    browseButtonText?: string;
    /** Text for the Clear button */
    clearButtonText?: string;
    /** Flag to hide the built-in preview of the file (where available).
     * If true, you can use children to render an alternate preview. */
    hideDefaultPreview?: boolean;
    /** Flag to allow editing of a text file's contents after it is selected from disk */
    allowEditingUploadedText?: boolean;
    /** Additional children to render after (or instead of) the file preview. */
    children?: React.ReactNode;
    /** A callback for when a selected file starts loading */
    onReadStarted?: (fileHandle: File) => void;
    /** A callback for when a selected file finishes loading */
    onReadFinished?: (fileHandle: File) => void;
    /** A callback for when the FileReader API fails */
    onReadFailed?: (error: DOMException, fileHandle: File) => void;
    /** Optional extra props to customize react-dropzone. */
    dropzoneProps?: DropzoneProps;
    /** Clear button was clicked */
    onClearClick?: React.MouseEventHandler<HTMLButtonElement>;
    /** Text area text changed */
    onTextChange?: (text: string) => void;
    /** On data changed - if type='text' or type='dataURL' and file was loaded it will call this method */
    onDataChange?: (data: string) => void;
}
export declare const FileUpload: React.FunctionComponent<FileUploadProps>;
//# sourceMappingURL=FileUpload.d.ts.map