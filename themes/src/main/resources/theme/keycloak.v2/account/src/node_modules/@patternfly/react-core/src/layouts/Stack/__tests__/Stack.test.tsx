import * as React from 'react';
import { render } from '@testing-library/react';
import { Stack } from '../Stack';
import { StackItem } from '../StackItem';

test('isMain set to true', () => {
  const { asFragment } = render(
    <Stack>
      <StackItem isFilled>Filled content</StackItem>
    </Stack>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('isMain defaults to false', () => {
  const { asFragment } = render(
    <Stack>
      <StackItem>Basic content</StackItem>
    </Stack>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('gutter', () => {
  const { asFragment } = render(
    <Stack hasGutter>
      <StackItem>Basic content</StackItem>
    </Stack>
  );
  expect(asFragment()).toMatchSnapshot();
});
