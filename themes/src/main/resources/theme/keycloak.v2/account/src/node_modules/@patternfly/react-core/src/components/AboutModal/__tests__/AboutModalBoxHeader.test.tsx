import * as React from 'react';
import { render } from '@testing-library/react';
import { AboutModalBoxHeader } from '../AboutModalBoxHeader';

test('AboutModalBoxHeader Test', () => {
  const { asFragment } = render(
    <AboutModalBoxHeader productName="Product Name" id="id">
      This is a AboutModalBox header
    </AboutModalBoxHeader>
  );
  expect(asFragment()).toMatchSnapshot();
});
