import * as React from 'react';
import { Level } from '../Level';
import { LevelItem } from '../LevelItem';
import { render } from '@testing-library/react';

test('Gutter', () => {
  const { asFragment } = render(<Level hasGutter />);
  expect(asFragment()).toMatchSnapshot();
});

test('item', () => {
  const { asFragment } = render(<LevelItem>Level Item</LevelItem>);
  expect(asFragment()).toMatchSnapshot();
});
