import React from 'react';
import { render } from '@testing-library/react';
import { Backdrop } from '../Backdrop';

test('Backdrop Test', () => {
  const { asFragment } = render(<Backdrop>Backdrop</Backdrop>);
  expect(asFragment()).toMatchSnapshot();
});
