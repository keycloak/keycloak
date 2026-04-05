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
