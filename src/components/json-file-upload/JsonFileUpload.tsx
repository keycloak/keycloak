import { Language } from "@patternfly/react-code-editor";

import { FileUploadForm, FileUploadFormProps } from "./FileUploadForm";

export type JsonFileUploadProps = Omit<
  FileUploadFormProps,
  "onChange" | "language" | "extension"
> & {
  onChange: (obj: object) => void;
};

export const JsonFileUpload = ({ onChange, ...props }: JsonFileUploadProps) => {
  const handleChange = (value: string) => {
    try {
      onChange(JSON.parse(value));
    } catch (error) {
      onChange({});
      console.warn("Invalid json, ignoring value using {}");
    }
  };

  return (
    <FileUploadForm
      {...props}
      language={Language.json}
      extension=".json"
      onChange={handleChange}
    />
  );
};
