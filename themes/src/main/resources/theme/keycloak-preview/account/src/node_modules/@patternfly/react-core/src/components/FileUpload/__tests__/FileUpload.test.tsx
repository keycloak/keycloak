import { FileUpload } from '../FileUpload';
import * as React from 'react';
import { shallow } from 'enzyme';

test('simple fileupload', () => {
  const changeHandler = jest.fn();
  const readStartedHandler = jest.fn();
  const readFinishedHandler = jest.fn();

  const view = shallow(
    <FileUpload
      id="simple-text-file"
      type="text"
      value={''}
      filename={''}
      onChange={changeHandler}
      onReadStarted={readStartedHandler}
      onReadFinished={readFinishedHandler}
      isLoading={false}
    />
  );
  expect(view).toMatchSnapshot();
});
