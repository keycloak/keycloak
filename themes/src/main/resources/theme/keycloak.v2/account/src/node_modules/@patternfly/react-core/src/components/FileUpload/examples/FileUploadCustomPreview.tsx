import React from 'react';
import { FileUpload } from '@patternfly/react-core';
import FileUploadIcon from '@patternfly/react-icons/dist/esm/icons/file-upload-icon';

export const CustomPreviewFileUpload: React.FunctionComponent = () => {
  const [value, setValue] = React.useState(null);
  const [filename, setFilename] = React.useState('');

  const handleFileInputChange = (
    _event: React.ChangeEvent<HTMLInputElement> | React.DragEvent<HTMLElement>,
    file: File
  ) => {
    setValue(file);
    setFilename(file.name);
  };

  const handleClear = (_event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    setFilename('');
    setValue('');
  };

  return (
    <FileUpload
      id="customized-preview-file"
      value={value}
      filename={filename}
      filenamePlaceholder="Drag and drop a file or upload one"
      onFileInputChange={handleFileInputChange}
      onClearClick={handleClear}
      hideDefaultPreview
      browseButtonText="Upload"
    >
      {value && (
        <div className="pf-u-m-md">
          <FileUploadIcon size="lg" /> Custom preview here for your {value.size}-byte file named {value.name}
        </div>
      )}
    </FileUpload>
  );
};
