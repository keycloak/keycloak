import { FileUploadField } from '../FileUploadField';
import * as React from 'react';
import { shallow } from 'enzyme';

test('simple fileuploadfield', () => {
  const changeHandler = jest.fn();
  const browserBtnClickHandler = jest.fn();
  const clearBtnClickHandler = jest.fn();

  const view = shallow(
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
  expect(view).toMatchSnapshot();
});
