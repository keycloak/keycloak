import * as React from 'react';
import { mount } from 'enzyme';
import { Stack } from '../Stack';
import { StackItem } from '../StackItem';
import { GutterSize } from '../../../styles/gutters';

test('isMain set to true', () => {
  const view = mount(
    <Stack>
      <StackItem isFilled>Filled content</StackItem>
    </Stack>
  );
  expect(view).toMatchSnapshot();
});

test('isMain defaults to false', () => {
  const view = mount(
    <Stack>
      <StackItem>Basic content</StackItem>
    </Stack>
  );
  expect(view).toMatchSnapshot();
});

Object.values(GutterSize).forEach(gutter => {
  test(`Gutter ${gutter}`, () => {
    const view = mount(
      <Stack gutter={gutter}>
        <StackItem>Basic content</StackItem>
      </Stack>
    );
    expect(view).toMatchSnapshot();
  });
});
