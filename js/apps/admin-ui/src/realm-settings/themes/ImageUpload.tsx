import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { FileUpload } from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";

type ImageUploadProps = {
  name: string;
  onChange?: (file: string) => void;
};

export const ImageUpload = ({ name, onChange }: ImageUploadProps) => {
  const [dataUri, setDataUri] = useState("");
  const [file, setFile] = useState<File>();
  const [isLoading, setIsLoading] = useState(false);

  const { control, watch } = useFormContext();

  const fileToDataUri = (file: File) =>
    new Promise<string>((resolve) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        resolve(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    });

  if (file) {
    void fileToDataUri(file).then((dataUri) => {
      setDataUri(dataUri);
      onChange?.(dataUri);
    });
  }

  const loadedFile = watch(name);
  useEffect(() => {
    (() => {
      if (loadedFile) {
        void fileToDataUri(loadedFile).then((dataUri) => {
          setDataUri(dataUri);
        });
      }
    })();
  }, [loadedFile]);

  return (
    <Controller
      name={name}
      control={control}
      defaultValue=""
      render={({ field }) => (
        <>
          {isLoading && <KeycloakSpinner />}
          {dataUri && <img src={dataUri} width={200} height={200} />}
          <FileUpload
            id={name}
            type="dataURL"
            filename={file?.name}
            dropzoneProps={{
              accept: {
                "image/*": [".png", ".gif", ".jpeg", ".jpg", ".svg", ".webp"],
              },
            }}
            onFileInputChange={(_, file) => setFile(file)}
            onReadStarted={() => setIsLoading(true)}
            onReadFinished={(_, file) => {
              setFile(file);
              field.onChange(file);
              setIsLoading(false);
            }}
            onClearClick={() => {
              setFile(undefined);
              field.onChange(undefined);
              setDataUri("");
            }}
          />
        </>
      )}
    />
  );
};
