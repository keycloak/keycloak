import React from 'react';
import { FileUploadField, Checkbox } from '@patternfly/react-core';

export const CustomPreviewFileUpload: React.FunctionComponent = () => {
  const properties = [
    'filename',
    'isClearButtonDisabled',
    'isDragActive',
    'isLoading',
    'hideDefaultPreview',
    'children',
    'hasPlaceholderText'
  ];

  const [value, setValue] = React.useState('');
  const [filename, setFilename] = React.useState(false);
  const [isClearButtonDisabled, setIsClearButtonDisabled] = React.useState(true);
  const [isLoading, setIsLoading] = React.useState(false);
  const [isDragActive, setIsDragActive] = React.useState(false);
  const [hideDefaultPreview, setHideDefaultPreview] = React.useState(false);
  const [children, setChildren] = React.useState(false);
  const [hasPlaceholderText, setHasPlaceholderText] = React.useState(false);
  const [checkedState, setCheckedState] = React.useState([
    filename,
    isClearButtonDisabled,
    isLoading,
    isDragActive,
    hideDefaultPreview,
    children,
    hasPlaceholderText
  ]);

  const handleTextAreaChange = (value: string) => {
    setValue(value);
  };

  const handleOnChange = (checked: boolean, stateKey: string, index: number) => {
    const updatedCheckedState = [...checkedState];
    switch (stateKey) {
      case 'filename':
        checked ? setFilename(true) : setFilename(false);
        break;

      case 'isClearButtonDisabled':
        checked ? setIsClearButtonDisabled(true) : setIsClearButtonDisabled(false);
        break;

      case 'isDragActive':
        checked ? setIsDragActive(true) : setIsDragActive(false);
        break;

      case 'isLoading':
        checked ? setIsLoading(true) : setIsLoading(false);
        // See notes above this example
        if (checked) {
          updatedCheckedState[properties.indexOf('hideDefaultPreview')] = false;
          setHideDefaultPreview(false);
        }
        break;

      case 'hideDefaultPreview':
        checked ? setHideDefaultPreview(true) : setHideDefaultPreview(false);
        // See notes above this example
        if (checked) {
          updatedCheckedState[properties.indexOf('isLoading')] = false;
          setIsLoading(false);
        }
        break;

      case 'children':
        checked ? setChildren(true) : setChildren(false);
        break;

      case 'hasPlaceholderText':
        checked ? setHasPlaceholderText(true) : setHasPlaceholderText(false);
        break;
    }
    updatedCheckedState[index] = checked;
    setCheckedState(updatedCheckedState);
  };

  return (
    <div>
      {properties.map((stateKey, index) => (
        <Checkbox
          name={stateKey}
          key={stateKey}
          id={stateKey}
          label={stateKey}
          aria-label={stateKey}
          isChecked={checkedState[index]}
          onChange={checked => handleOnChange(checked, stateKey, index)}
        />
      ))}
      <br />
      <FileUploadField
        id="custom-file-upload"
        type="text"
        value={value}
        filename={filename ? 'example-filename.txt' : ''}
        onTextChange={handleTextAreaChange}
        filenamePlaceholder="Do something custom with this!"
        onBrowseButtonClick={() => alert('Browse button clicked!')}
        onClearButtonClick={() => alert('Clear button clicked!')}
        isClearButtonDisabled={isClearButtonDisabled}
        isLoading={isLoading}
        isDragActive={isDragActive}
        hideDefaultPreview={hideDefaultPreview}
        browseButtonText="Upload"
        textAreaPlaceholder={hasPlaceholderText ? 'File preview' : ''}
      >
        {children && <div className="pf-u-m-md">(A custom preview of the uploaded file can be passed as children)</div>}
      </FileUploadField>
    </div>
  );
};
