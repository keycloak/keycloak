import React from 'react';
import { FileUpload, Form, FormGroup } from '@patternfly/react-core';

export const TextFileUploadWithRestrictions: React.FunctionComponent = () => {
  const [value, setValue] = React.useState('');
  const [filename, setFilename] = React.useState('');
  const [isLoading, setIsLoading] = React.useState(false);
  const [isRejected, setIsRejected] = React.useState(false);

  const handleFileInputChange = (
    _event: React.ChangeEvent<HTMLInputElement> | React.DragEvent<HTMLElement>,
    file: File
  ) => {
    setFilename(file.name);
  };

  const handleTextOrDataChange = (value: string) => {
    setValue(value);
  };

  const handleClear = (_event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    setFilename('');
    setValue('');
    setIsRejected(false);
  };

  const handleFileRejected = (_rejectedFiles: File[], _event: React.DragEvent<HTMLElement>) => {
    setIsRejected(true);
  };

  const handleFileReadStarted = (_fileHandle: File) => {
    setIsLoading(true);
  };

  const handleFileReadFinished = (_fileHandle: File) => {
    setIsLoading(false);
  };

  return (
    <Form>
      <FormGroup
        fieldId="text-file-with-restrictions"
        helperText="Upload a CSV file"
        helperTextInvalid="Must be a CSV file no larger than 1 KB"
        validated={isRejected ? 'error' : 'default'}
      >
        <FileUpload
          id="text-file-with-restrictions"
          type="text"
          value={value}
          filename={filename}
          filenamePlaceholder="Drag and drop a file or upload one"
          onFileInputChange={handleFileInputChange}
          onDataChange={handleTextOrDataChange}
          onTextChange={handleTextOrDataChange}
          onReadStarted={handleFileReadStarted}
          onReadFinished={handleFileReadFinished}
          onClearClick={handleClear}
          isLoading={isLoading}
          dropzoneProps={{
            accept: '.csv',
            maxSize: 1024,
            onDropRejected: handleFileRejected
          }}
          validated={isRejected ? 'error' : 'default'}
          browseButtonText="Upload"
        />
      </FormGroup>
    </Form>
  );
};
