import * as React from 'react';
import Dropzone, { DropzoneProps, DropFileEventHandler } from 'react-dropzone';
import { FileUploadField, FileUploadFieldProps } from './FileUploadField';
import { readFile, fileReaderType } from '../../helpers/fileUtils';

export interface FileUploadProps
  extends Omit<
    FileUploadFieldProps,
    'children' | 'onBrowseButtonClick' | 'onClearButtonClick' | 'isDragActive' | 'containerRef'
  > {
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
  /** A callback for when the file contents change. */
  onChange?: (
    value: string | File,
    filename: string,
    event:
      | React.DragEvent<HTMLElement> // User dragged/dropped a file
      | React.ChangeEvent<HTMLTextAreaElement> // User typed in the TextArea
      | React.MouseEvent<HTMLButtonElement, MouseEvent> // User clicked Clear button
  ) => void;
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
  /* Value to indicate if the field is modified to show that validation state.
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

  // Props available in FileUpload but not FileUploadField:

  /** A callback for when a selected file starts loading */
  onReadStarted?: (fileHandle: File) => void;
  /** A callback for when a selected file finishes loading */
  onReadFinished?: (fileHandle: File) => void;
  /** A callback for when the FileReader API fails */
  onReadFailed?: (error: DOMException, fileHandle: File) => void;
  /** Optional extra props to customize react-dropzone. */
  dropzoneProps?: DropzoneProps;
}

export const FileUpload: React.FunctionComponent<FileUploadProps> = ({
  id,
  type,
  value = type === fileReaderType.text || type === fileReaderType.dataURL ? '' : null,
  filename = '',
  children = null,
  onChange = () => {},
  onReadStarted = () => {},
  onReadFinished = () => {},
  onReadFailed = () => {},
  dropzoneProps = {},
  ...props
}: FileUploadProps) => {
  const onDropAccepted: DropFileEventHandler = (acceptedFiles: File[], event: React.DragEvent<HTMLElement>) => {
    if (acceptedFiles.length > 0) {
      const fileHandle = acceptedFiles[0];
      if (type === fileReaderType.text || type === fileReaderType.dataURL) {
        onChange('', fileHandle.name, event); // Show the filename while reading
        onReadStarted(fileHandle);
        readFile(fileHandle, type as fileReaderType)
          .then(data => {
            onReadFinished(fileHandle);
            onChange(data as string, fileHandle.name, event);
          })
          .catch((error: DOMException) => {
            onReadFailed(error, fileHandle);
            onReadFinished(fileHandle);
            onChange('', '', event); // Clear the filename field on a failure
          });
      } else {
        onChange(fileHandle, fileHandle.name, event);
      }
    }
    dropzoneProps.onDropAccepted && dropzoneProps.onDropAccepted(acceptedFiles, event);
  };

  const onDropRejected: DropFileEventHandler = (rejectedFiles: File[], event: React.DragEvent<HTMLElement>) => {
    if (rejectedFiles.length > 0) {
      onChange('', rejectedFiles[0].name, event);
    }
    dropzoneProps.onDropRejected && dropzoneProps.onDropRejected(rejectedFiles, event);
  };

  const onClearButtonClick = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    onChange('', '', event);
  };

  return (
    <Dropzone multiple={false} {...dropzoneProps} onDropAccepted={onDropAccepted} onDropRejected={onDropRejected}>
      {({ getRootProps, getInputProps, isDragActive, open }) => (
        <FileUploadField
          {...getRootProps({
            ...props,
            refKey: 'containerRef',
            onClick: event => event.preventDefault() // Prevents clicking TextArea from opening file dialog
          })}
          tabIndex={null} // Omit the unwanted tabIndex from react-dropzone's getRootProps
          id={id}
          type={type}
          filename={filename}
          value={value}
          onChange={onChange}
          isDragActive={isDragActive}
          onBrowseButtonClick={open}
          onClearButtonClick={onClearButtonClick}
        >
          <input {...getInputProps()} /* hidden, necessary for react-dropzone */ />
          {children}
        </FileUploadField>
      )}
    </Dropzone>
  );
};
