import React from 'react';
import { FileUpload } from '@patternfly/react-core';

export const SimpleFileUpload: React.FunctionComponent = () => {
  const [value, setValue] = React.useState(null);
  const [filename, setFilename] = React.useState('');

  const handleFileInputChange = (
    _event: React.ChangeEvent<HTMLInputElement> | React.DragEvent<HTMLElement>,
    file: File
  ) => {
    setFilename(file.name);
  };

  const handleClear = (_event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    setFilename('');
    setValue('');
  };

  return (
    <FileUpload
      id="simple-file"
      value={value}
      filename={filename}
      filenamePlaceholder="Drag and drop a file or upload one"
      onFileInputChange={handleFileInputChange}
      onClearClick={handleClear}
      browseButtonText="Upload"
    />
  );
};
