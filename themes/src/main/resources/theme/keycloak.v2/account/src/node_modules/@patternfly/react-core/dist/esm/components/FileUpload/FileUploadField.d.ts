import * as React from 'react';
export interface FileUploadFieldProps extends Omit<React.HTMLProps<HTMLDivElement>, 'value' | 'onChange'> {
    /** Unique id for the TextArea, also used to generate ids for accessible labels */
    id: string;
    /** What type of file. Determines what is is expected by `value`
     * (a string for 'text' and 'dataURL', or a File object otherwise). */
    type?: 'text' | 'dataURL';
    /** Value of the file's contents
     * (string if text file, File object otherwise) */
    value?: string | File;
    /** Value to be shown in the read-only filename field. */
    filename?: string;
    /** A callback for when the TextArea value changes. */
    onChange?: (value: string, filename: string, event: React.ChangeEvent<HTMLTextAreaElement> | React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
    /** Additional classes added to the FileUploadField container element. */
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
    /** Flag to disable the Clear button */
    isClearButtonDisabled?: boolean;
    /** Flag to hide the built-in preview of the file (where available).
     * If true, you can use children to render an alternate preview. */
    hideDefaultPreview?: boolean;
    /** Flag to allow editing of a text file's contents after it is selected from disk */
    allowEditingUploadedText?: boolean;
    /** Additional children to render after (or instead of) the file preview. */
    children?: React.ReactNode;
    /** A callback for when the Browse button is clicked. */
    onBrowseButtonClick?: (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
    /** A callback for when the Clear button is clicked. */
    onClearButtonClick?: (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => void;
    /** A callback from when the text area is clicked. Can also be set via the onClick property of FileUpload. */
    onTextAreaClick?: (event: React.MouseEvent<HTMLTextAreaElement, MouseEvent>) => void;
    /** Flag to show if a file is being dragged over the field */
    isDragActive?: boolean;
    /** A reference object to attach to the FileUploadField container element. */
    containerRef?: React.Ref<HTMLDivElement>;
    /** Text area text changed */
    onTextChange?: (text: string) => void;
    /** Callback for when focus is lost on the text area field */
    onTextAreaBlur?: (event?: any) => void;
    /** Placeholder string to display in the empty text area field */
    textAreaPlaceholder?: string;
}
export declare const FileUploadField: React.FunctionComponent<FileUploadFieldProps>;
//# sourceMappingURL=FileUploadField.d.ts.map