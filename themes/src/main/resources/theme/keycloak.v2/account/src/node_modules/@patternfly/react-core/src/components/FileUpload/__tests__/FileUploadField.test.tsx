import { FileUploadField } from '../FileUploadField';
import * as React from 'react';
import { render } from '@testing-library/react';

test('simple fileuploadfield', () => {
  const changeHandler = jest.fn();
  const browserBtnClickHandler = jest.fn();
  const clearBtnClickHandler = jest.fn();

  const { asFragment } = render(
    <FileUploadField
      id="custom-file-upload"
      type="text"
      value={''}
      filename={''}
      onChange={changeHandler}
      filenamePlaceholder="Do something custom with this!"
      onBrowseButtonClick={browserBtnClickHandler}
      onClearButtonClick={clearBtnClickHandler}
      isClearButtonDisabled={false}
      isLoading={false}
      isDragActive={false}
      hideDefaultPreview={false}
    >
      {<p>A custom preview of the uploaded file can be passed as children</p>}
    </FileUploadField>
  );
  expect(asFragment()).toMatchSnapshot();
});
