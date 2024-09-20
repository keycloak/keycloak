import { CodeEditor, Language } from "@patternfly/react-code-editor";
import {
  Button,
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import {
  ChangeEvent,
  DragEvent as ReactDragEvent,
  MouseEvent as ReactMouseEvent,
  useState,
} from "react";
import { DropEvent } from "react-dropzone";
import { useTranslation } from "react-i18next";
import { FileUpload, FileUploadProps } from "./patternfly/FileUpload";

type FileUploadType = {
  value: string;
  filename: string;
  isLoading: boolean;
  modal: boolean;
};

export type FileUploadEvent =
  | ReactDragEvent<HTMLElement> // User dragged/dropped a file
  | ChangeEvent<HTMLTextAreaElement> // User typed in the TextArea
  | ReactMouseEvent<HTMLButtonElement, MouseEvent>; // User clicked Clear button

export type FileUploadFormProps = Omit<FileUploadProps, "onChange"> & {
  id: string;
  extension: string;
  onChange: (value: string) => void;
  helpText?: string;
  unWrap?: boolean;
  language?: Language;
};

export const FileUploadForm = ({
  id,
  onChange,
  helpText = "helpFileUpload",
  unWrap = false,
  language,
  extension,
  ...rest
}: FileUploadFormProps) => {
  const { t } = useTranslation();
  const defaultUpload: FileUploadType = {
    value: "",
    filename: "",
    isLoading: false,
    modal: false,
  };
  const [fileUpload, setFileUpload] = useState<FileUploadType>(defaultUpload);
  const removeDialog = () => setFileUpload({ ...fileUpload, modal: false });

  const handleFileInputChange = (_event: DropEvent, file: File) => {
    setFileUpload({ ...fileUpload, filename: file.name });
  };

  const handleTextOrDataChange = (value: string) => {
    setFileUpload({ ...fileUpload, value });
    onChange(value);
  };

  const handleClear = () => {
    setFileUpload({ ...fileUpload, modal: true });
  };

  return (
    <>
      {fileUpload.modal && (
        <Modal
          variant={ModalVariant.small}
          title={t("clearFile")}
          isOpen
          onClose={removeDialog}
          actions={[
            <Button
              key="confirm"
              variant="primary"
              data-testid="clear-button"
              onClick={() => {
                setFileUpload(defaultUpload);
                onChange("");
              }}
            >
              {t("clear")}
            </Button>,
            <Button
              data-testid="cancel"
              key="cancel"
              variant="link"
              onClick={removeDialog}
            >
              {t("cancel")}
            </Button>,
          ]}
        >
          {t("clearFileExplain")}
        </Modal>
      )}
      {unWrap && (
        <FileUpload
          id={id}
          {...rest}
          type="text"
          value={fileUpload.value}
          filename={fileUpload.filename}
          onFileInputChange={handleFileInputChange}
          onDataChange={handleTextOrDataChange}
          onTextChange={handleTextOrDataChange}
          onClearClick={handleClear}
          onReadStarted={() =>
            setFileUpload({ ...fileUpload, isLoading: true })
          }
          onReadFinished={() =>
            setFileUpload({ ...fileUpload, isLoading: false })
          }
          isLoading={fileUpload.isLoading}
          dropzoneProps={{
            accept: { "application/text": [extension] },
          }}
        />
      )}
      {!unWrap && (
        <FormGroup label={t("resourceFile")} fieldId={id}>
          <FileUpload
            data-testid={id}
            id={id}
            {...rest}
            type="text"
            value={fileUpload.value}
            filename={fileUpload.filename}
            onFileInputChange={handleFileInputChange}
            onDataChange={handleTextOrDataChange}
            onTextChange={handleTextOrDataChange}
            onClearClick={handleClear}
            onReadStarted={() =>
              setFileUpload({ ...fileUpload, isLoading: true })
            }
            onReadFinished={() =>
              setFileUpload({ ...fileUpload, isLoading: false })
            }
            isLoading={fileUpload.isLoading}
            hideDefaultPreview
          >
            {!rest.hideDefaultPreview && (
              <CodeEditor
                isLineNumbersVisible
                code={fileUpload.value}
                language={language}
                height="128px"
                onChange={handleTextOrDataChange}
                isReadOnly={!rest.allowEditingUploadedText}
              />
            )}
          </FileUpload>
          <FormHelperText>
            <HelperText>
              <HelperTextItem>{t(helpText)}</HelperTextItem>
            </HelperText>
          </FormHelperText>
        </FormGroup>
      )}
    </>
  );
};
