import {
  Button,
  DropEvent,
  FileUpload,
  FileUploadProps,
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
import { useTranslation } from "react-i18next";
import CodeEditor from "../form/CodeEditor";

type FileUploadType = {
  value: string;
  filename: string;
  isLoading: boolean;
  modal: boolean;
};

export type JsonFileUploadProps = Omit<
  FileUploadFormProps,
  "onChange" | "language" | "extension"
> & {
  onChange: (obj: object) => void;
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
  language?: string;
};

export const FileUploadForm = ({
  id,
  onChange,
  helpText = "helpFileUpload",
  unWrap = false,
  language,
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
          onDataChange={(_, value) => handleTextOrDataChange(value)}
          onTextChange={(_, value) => handleTextOrDataChange(value)}
          onClearClick={handleClear}
          onReadStarted={() =>
            setFileUpload({ ...fileUpload, isLoading: true })
          }
          onReadFinished={() =>
            setFileUpload({ ...fileUpload, isLoading: false })
          }
          isLoading={fileUpload.isLoading}
          dropzoneProps={{
            accept: { "application/json": [".json"] },
          }}
        />
      )}
      {!unWrap && (
        <FormGroup label={t("resourceFile")} fieldId={id + "-filename"}>
          <FileUpload
            data-testid={id}
            id={id}
            {...rest}
            type="text"
            value={fileUpload.value}
            filename={fileUpload.filename}
            onFileInputChange={handleFileInputChange}
            onDataChange={(_, value) => handleTextOrDataChange(value)}
            onTextChange={(_, value) => handleTextOrDataChange(value)}
            onClearClick={handleClear}
            onReadStarted={() =>
              setFileUpload({ ...fileUpload, isLoading: true })
            }
            onReadFinished={() =>
              setFileUpload({ ...fileUpload, isLoading: false })
            }
            isLoading={fileUpload.isLoading}
            hideDefaultPreview
            dropzoneProps={{
              accept: { "application/json": [".json"] },
            }}
          >
            {!rest.hideDefaultPreview && (
              <CodeEditor
                aria-label="File content"
                value={fileUpload.value}
                language={language}
                onChange={(value) => handleTextOrDataChange(value)}
                readOnly={!rest.allowEditingUploadedText}
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

export const JsonFileUpload = ({ onChange, ...props }: JsonFileUploadProps) => {
  const handleChange = (value: string) => {
    try {
      onChange(JSON.parse(value));
    } catch {
      onChange({});
      console.warn("Invalid json, ignoring value using {}");
    }
  };

  return (
    <FileUploadForm
      {...props}
      language="json"
      extension=".json"
      onChange={handleChange}
    />
  );
};
