import { useState } from "react";
import { useTranslation } from "react-i18next";
import { FileUploadForm, FileUploadFormProps } from "./FileUploadForm";

export type JsonFileUploadProps = Omit<
  FileUploadFormProps,
  "onChange" | "language" | "extension" | "errorText"
> & {
  onChange: (obj: object) => void;
  /** Called with the parse error when the content is not valid JSON, and with undefined once it is. */
  onParseError?: (error?: string) => void;
};

export const JsonFileUpload = ({
  onChange,
  onParseError,
  ...props
}: JsonFileUploadProps) => {
  const { t } = useTranslation();
  const [error, setError] = useState<string>();

  const updateError = (message?: string) => {
    setError(message);
    onParseError?.(message);
  };

  const handleChange = (value: string) => {
    if (value.trim() === "") {
      updateError(undefined);
      onChange({});
      return;
    }

    try {
      onChange(JSON.parse(value));
      updateError(undefined);
    } catch (parseError) {
      onChange({});
      updateError(
        parseError instanceof Error ? parseError.message : String(parseError),
      );
    }
  };

  return (
    <FileUploadForm
      {...props}
      language="json"
      extension=".json"
      onChange={handleChange}
      errorText={error && t("invalidJsonFile", { error })}
    />
  );
};
