import * as React from 'react';
import { render } from '@testing-library/react';
import { AboutModalBoxContent } from '../AboutModalBoxContent';

test('AboutModalBoxContent Test', () => {
  const { asFragment } = render(
    <AboutModalBoxContent trademark="trademark" id="id">
      This is a AboutModalBoxContent
    </AboutModalBoxContent>
  );
  expect(asFragment()).toMatchSnapshot();
});
