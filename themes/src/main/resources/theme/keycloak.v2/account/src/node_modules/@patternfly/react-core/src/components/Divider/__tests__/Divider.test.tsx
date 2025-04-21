import { Divider } from '../Divider';
import { Flex, FlexItem } from '../../../layouts/Flex';
import * as React from 'react';
import { render } from '@testing-library/react';

test('divider using hr', () => {
  const { asFragment } = render(<Divider />);
  expect(asFragment()).toMatchSnapshot();
});

test('divider using li', () => {
  const { asFragment } = render(<Divider component="li" />);
  expect(asFragment()).toMatchSnapshot();
});

test('divider using div', () => {
  const { asFragment } = render(<Divider component="div" />);
  expect(asFragment()).toMatchSnapshot();
});

test('vertical divider', () => {
  const { asFragment } = render(
    <Flex>
      <FlexItem>first item</FlexItem>
      <Divider
        orientation={{
          default: 'vertical'
        }}
      />
      <FlexItem>second item</FlexItem>
    </Flex>
  );
  expect(asFragment()).toMatchSnapshot();
});
