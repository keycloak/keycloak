import { FileUpload } from '../FileUpload';
import * as React from 'react';
import { render } from '@testing-library/react';

test('simple fileupload', () => {
  const changeHandler = jest.fn();
  const readStartedHandler = jest.fn();
  const readFinishedHandler = jest.fn();

  const { asFragment } = render(
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
  expect(asFragment()).toMatchSnapshot();
});
