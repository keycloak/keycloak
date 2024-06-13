import { fromEvent } from "file-selector";
import { PropsWithChildren } from "react";
import {
  DropEvent,
  DropzoneInputProps,
  DropzoneOptions,
  FileRejection,
  useDropzone,
} from "react-dropzone";

import { FileUploadField, FileUploadFieldProps } from "./FileUploadField";
import { fileReaderType, readFile } from "./fileUtils";

export interface FileUploadProps
  extends Omit<
    FileUploadFieldProps,
    | "children"
    | "onBrowseButtonClick"
    | "onClearButtonClick"
    | "isDragActive"
    | "containerRef"
  > {
  /** Unique id for the TextArea, also used to generate ids for accessible labels. */
  id: string;
  /** What type of file. Determines what is is passed to `onChange` and expected by `value`
   * (a string for 'text' and 'dataURL', or a File object otherwise. */
  type?: "text" | "dataURL";
  /** Value of the file's contents
   * (string if text file, File object otherwise) */
  value?: string | File;
  /** Value to be shown in the read-only filename field. */
  filename?: string;
  /** @deprecated A callback for when the file contents change. Please instead use onFileInputChange, onTextChange, onDataChange, onClearClick individually.  */
  onChange?: (
    value: string | File,
    filename: string,
    event:
      | React.MouseEvent<HTMLButtonElement, MouseEvent> // Clear button was clicked
      | React.ChangeEvent<HTMLElement> // User typed in the TextArea
      | DropEvent,
  ) => void;
  /** Change event emitted from the hidden \<input type="file" \> field associated with the component  */
  onFileInputChange?: (event: DropEvent, file: File) => void;
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
  validated?: "success" | "error" | "default";
  /** Aria-label for the TextArea. */
  "aria-label"?: string;
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
  dropzoneProps?: DropzoneOptions;
  /** Clear button was clicked */
  onClearClick?: React.MouseEventHandler<HTMLButtonElement>;
  /** Text area text changed */
  onTextChange?: (text: string) => void;
  /** On data changed - if type='text' or type='dataURL' and file was loaded it will call this method */
  onDataChange?: (data: string) => void;
}

export const FileUpload = ({
  id,
  type,
  value = type === fileReaderType.text || type === fileReaderType.dataURL
    ? ""
    : undefined,
  filename = "",
  children = null,
  onChange,
  onFileInputChange,
  onReadStarted,
  onReadFinished,
  onReadFailed,
  onClearClick,
  onClick = (event) => event.preventDefault(),
  onTextChange,
  onDataChange,
  dropzoneProps = {},
  ...props
}: PropsWithChildren<FileUploadProps>) => {
  const onDropAccepted = (acceptedFiles: File[], event: DropEvent) => {
    if (acceptedFiles.length > 0) {
      const fileHandle = acceptedFiles[0];
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (event?.type === "drop") {
        onFileInputChange?.(event, fileHandle);
      }
      if (type === fileReaderType.text || type === fileReaderType.dataURL) {
        onChange?.("", fileHandle.name, event); // Show the filename while reading
        onReadStarted?.(fileHandle);
        readFile(fileHandle, type as fileReaderType)
          .then((data) => {
            onReadFinished?.(fileHandle);
            onChange?.(data as string, fileHandle.name, event);
            onDataChange?.(data as string);
          })
          .catch((error: DOMException) => {
            onReadFailed?.(error, fileHandle);
            onReadFinished?.(fileHandle);
            onChange?.("", "", event); // Clear the filename field on a failure
            onDataChange?.("");
          });
      } else {
        onChange?.(fileHandle, fileHandle.name, event);
      }
    }
    dropzoneProps.onDropAccepted?.(acceptedFiles, event);
  };

  const onDropRejected = (rejectedFiles: FileRejection[], event: DropEvent) => {
    if (rejectedFiles.length > 0) {
      onChange?.("", rejectedFiles[0].file.name, event);
    }

    dropzoneProps.onDropRejected?.(rejectedFiles, event);
  };

  const onClearButtonClick = (
    event: React.MouseEvent<HTMLButtonElement, MouseEvent>,
  ) => {
    onChange?.("", "", event);
    onClearClick?.(event);
    setFileValue("");
  };

  const { getRootProps, getInputProps, isDragActive, open, inputRef } =
    useDropzone({
      multiple: false,
      ...dropzoneProps,
      onDropAccepted,
      onDropRejected,
    });

  const setFileValue = (filename: string) => {
    if (!inputRef.current) {
      return;
    }

    inputRef.current.value = filename;
  };

  const oldInputProps = getInputProps();
  const inputProps: DropzoneInputProps = {
    ...oldInputProps,
    onChange: async (e: React.ChangeEvent<HTMLInputElement>) => {
      oldInputProps.onChange?.(e);
      const files = await fromEvent(e.nativeEvent);
      if (files.length === 1) {
        onFileInputChange?.(e, files[0] as File);
      }
    },
  };

  return (
    <FileUploadField
      {...getRootProps({
        ...props,
        refKey: "containerRef",
        onClick: (event) => event.preventDefault(),
      })}
      tabIndex={undefined} // Omit the unwanted tabIndex from react-dropzone's getRootProps
      id={id}
      type={type}
      filename={filename}
      value={value}
      onChange={onChange}
      isDragActive={isDragActive}
      onBrowseButtonClick={open}
      onClearButtonClick={onClearButtonClick}
      onTextAreaClick={onClick}
      onTextChange={onTextChange}
      onClick={(e) => e.stopPropagation()}
    >
      <input
        /* hidden, necessary for react-dropzone */
        {...inputProps}
        ref={inputRef}
      />
      {children}
    </FileUploadField>
  );
};

FileUpload.displayName = "FileUpload";
