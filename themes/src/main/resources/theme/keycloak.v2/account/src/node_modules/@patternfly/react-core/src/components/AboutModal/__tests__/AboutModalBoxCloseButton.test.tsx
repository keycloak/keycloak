import * as React from 'react';
import { render } from '@testing-library/react';
import { AboutModalBoxCloseButton } from '../AboutModalBoxCloseButton';

test('AboutModalBoxCloseButton Test', () => {
  const { asFragment } = render(<AboutModalBoxCloseButton />);
  expect(asFragment()).toMatchSnapshot();
});

test('AboutModalBoxCloseButton Test onclose', () => {
  const onClose = jest.fn();
  const { asFragment } = render(<AboutModalBoxCloseButton onClose={onClose} />);
  expect(asFragment()).toMatchSnapshot();
});

test('AboutModalBoxCloseButton Test close button aria label', () => {
  const closeButtonAriaLabel = 'Klose Daylok';
  const { asFragment } = render(<AboutModalBoxCloseButton aria-label={closeButtonAriaLabel} />);
  expect(asFragment()).toMatchSnapshot();
});
