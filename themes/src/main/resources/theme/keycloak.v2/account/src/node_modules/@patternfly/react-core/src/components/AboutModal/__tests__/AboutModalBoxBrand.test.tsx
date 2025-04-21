import * as React from 'react';
import { render } from '@testing-library/react';
import { AboutModalBoxBrand } from '../AboutModalBoxBrand';

test('test About Modal Brand', () => {
  const { asFragment } = render(<AboutModalBoxBrand src="testimage.." alt="brand" />);
  expect(asFragment()).toMatchSnapshot();
});
