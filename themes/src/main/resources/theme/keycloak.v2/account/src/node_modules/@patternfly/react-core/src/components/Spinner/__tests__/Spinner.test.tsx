import * as React from 'react';
import { render } from '@testing-library/react';
import { Spinner } from '../Spinner';

test('simple spinner', () => {
  const { asFragment } = render(<Spinner />);
  expect(asFragment()).toMatchSnapshot();
});

test('small spinner', () => {
  const { asFragment } = render(<Spinner size="sm" />);
  expect(asFragment()).toMatchSnapshot();
});

test('medium spinner', () => {
  const { asFragment } = render(<Spinner size="md" />);
  expect(asFragment()).toMatchSnapshot();
});

test('large spinner', () => {
  const { asFragment } = render(<Spinner size="lg" />);
  expect(asFragment()).toMatchSnapshot();
});

test('extra large spinner', () => {
  const { asFragment } = render(<Spinner size="xl" />);
  expect(asFragment()).toMatchSnapshot();
});
