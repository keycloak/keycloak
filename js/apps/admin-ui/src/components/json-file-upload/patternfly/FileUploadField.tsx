import {
  Button,
  ButtonVariant,
  InputGroup,
  Spinner,
  spinnerSize,
  TextArea,
  TextAreResizeOrientation,
  TextInput,
  InputGroupItem,
} from "@patternfly/react-core";
import { css } from "@patternfly/react-styles";
import styles from "@patternfly/react-styles/css/components/FileUpload/file-upload";
import { PropsWithChildren } from "react";

import { fileReaderType } from "./fileUtils";

export interface FileUploadFieldProps
  extends Omit<React.HTMLProps<HTMLDivElement>, "value" | "onChange"> {
  /** Unique id for the TextArea, also used to generate ids for accessible labels */
  id: string;
  /** What type of file. Determines what is is expected by `value`
   * (a string for 'text' and 'dataURL', or a File object otherwise). */
  type?: "text" | "dataURL";
  /** Value of the file's contents
   * (string if text file, File object otherwise) */
  value?: string | File;
  /** Value to be shown in the read-only filename field. */
  filename?: string;
  /** A callback for when the TextArea value changes. */
  onChange?: (
    value: string,
    filename: string,
    event:
      | React.ChangeEvent<HTMLTextAreaElement> // User typed in the TextArea
      | React.MouseEvent<HTMLButtonElement, MouseEvent>, // User clicked Clear button
  ) => void;
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
  /** Flag to disable the Clear button */
  isClearButtonDisabled?: boolean;
  /** Flag to hide the built-in preview of the file (where available).
   * If true, you can use children to render an alternate preview. */
  hideDefaultPreview?: boolean;
  /** Flag to allow editing of a text file's contents after it is selected from disk */
  allowEditingUploadedText?: boolean;
  /** Additional children to render after (or instead of) the file preview. */
  children?: React.ReactNode;

  // Props available in FileUploadField but not FileUpload:

  /** A callback for when the Browse button is clicked. */
  onBrowseButtonClick?: (
    event: React.MouseEvent<HTMLButtonElement, MouseEvent>,
  ) => void;
  /** A callback for when the Clear button is clicked. */
  onClearButtonClick?: (
    event: React.MouseEvent<HTMLButtonElement, MouseEvent>,
  ) => void;
  /** A callback from when the text area is clicked. Can also be set via the onClick property of FileUpload. */
  onTextAreaClick?: (
    event: React.MouseEvent<HTMLTextAreaElement, MouseEvent>,
  ) => void;
  /** Flag to show if a file is being dragged over the field */
  isDragActive?: boolean;
  /** A reference object to attach to the FileUploadField container element. */
  containerRef?: React.Ref<HTMLDivElement>;
  /** Text area text changed */
  onTextChange?: (text: string) => void;
}

export const FileUploadField = ({
  id,
  type,
  value = "",
  filename = "",
  onChange,
  onBrowseButtonClick,
  onClearButtonClick,
  onTextAreaClick,
  onTextChange,
  className = "",
  isDisabled = false,
  isReadOnly = false,
  isLoading = false,
  spinnerAriaValueText,
  isRequired = false,
  isDragActive = false,
  validated = "default" as "success" | "error" | "default",
  "aria-label": ariaLabel = "File upload",
  filenamePlaceholder = "Drag a file here or browse to upload",
  filenameAriaLabel = filename ? "Read only filename" : filenamePlaceholder,
  browseButtonText = "Browse...",
  clearButtonText = "Clear",
  isClearButtonDisabled = !filename && !value,
  containerRef = null as React.Ref<HTMLDivElement>,
  allowEditingUploadedText = false,
  hideDefaultPreview = false,
  children = null,

  ...props
}: PropsWithChildren<FileUploadFieldProps>) => {
  const onTextAreaChange = (
    newValue: string,
    event: React.ChangeEvent<HTMLTextAreaElement>,
  ) => {
    onChange?.(newValue, filename, event);
    onTextChange?.(newValue);
  };
  return (
    <div
      className={css(
        styles.fileUpload,
        isDragActive && styles.modifiers.dragHover,
        isLoading && styles.modifiers.loading,
        className,
      )}
      ref={containerRef}
      {...props}
    >
      <div className={styles.fileUploadFileSelect}>
        <InputGroup>
          <InputGroupItem isFill>
            <TextInput
              // Always read-only regardless of isReadOnly prop (which is just for the TextArea)
              isDisabled={isDisabled}
              id={`${id}-filename`}
              name={`${id}-filename`}
              aria-label={filenameAriaLabel}
              placeholder={filenamePlaceholder}
              aria-describedby={`${id}-browse-button`}
              value={filename}
              readOnlyVariant="default"
            />
          </InputGroupItem>
          <InputGroupItem>
            <Button
              id={`${id}-browse-button`}
              variant={ButtonVariant.control}
              onClick={onBrowseButtonClick}
              isDisabled={isDisabled}
            >
              {browseButtonText}
            </Button>
          </InputGroupItem>
          <InputGroupItem>
            <Button
              variant={ButtonVariant.control}
              isDisabled={isDisabled || isClearButtonDisabled}
              onClick={onClearButtonClick}
            >
              {clearButtonText}
            </Button>
          </InputGroupItem>
        </InputGroup>
      </div>
      <div className={styles.fileUploadFileDetails}>
        {!hideDefaultPreview && type === fileReaderType.text && (
          <TextArea
            readOnly={isReadOnly || (!!filename && !allowEditingUploadedText)}
            disabled={isDisabled}
            isRequired={isRequired}
            resizeOrientation={TextAreResizeOrientation.vertical}
            validated={validated}
            id={id}
            name={id}
            aria-label={ariaLabel}
            value={value as string}
            onChange={(event, newValue: string) =>
              onTextAreaChange(newValue, event)
            }
            onClick={onTextAreaClick}
          />
        )}
        {isLoading && (
          <div className={styles.fileUploadFileDetailsSpinner}>
            <Spinner
              size={spinnerSize.lg}
              aria-valuetext={spinnerAriaValueText}
            />
          </div>
        )}
      </div>
      {children}
    </div>
  );
};
FileUploadField.displayName = "FileUploadField";
