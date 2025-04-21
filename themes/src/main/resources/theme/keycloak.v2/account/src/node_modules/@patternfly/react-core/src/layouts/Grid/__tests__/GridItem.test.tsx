import * as React from 'react';
import { GridItem } from '../GridItem';
import { render } from '@testing-library/react';
import { DeviceSizes } from '../../../styles/sizes';

test('adds span class', () => {
  const { asFragment } = render(<GridItem span={4} />);
  expect(asFragment()).toMatchSnapshot();
});

test('adds offset class', () => {
  const { asFragment } = render(<GridItem offset={4} />);
  expect(asFragment()).toMatchSnapshot();
});

test('adds row class', () => {
  const { asFragment } = render(<GridItem rowSpan={4} />);
  expect(asFragment()).toMatchSnapshot();
});

Object.keys(DeviceSizes).forEach(size => {
  test(`adds ${size} span class`, () => {
    const props = { [size]: 4 };
    const { asFragment } = render(<GridItem {...props} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test(`adds ${size} offset classes`, () => {
    const props = { [`${size}Offset`]: 1 };
    const { asFragment } = render(<GridItem {...props} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test(`adds ${size} row classes`, () => {
    const props = { [`${size}RowSpan`]: 1 };
    const { asFragment } = render(<GridItem {...props} />);
    expect(asFragment()).toMatchSnapshot();
  });
});
