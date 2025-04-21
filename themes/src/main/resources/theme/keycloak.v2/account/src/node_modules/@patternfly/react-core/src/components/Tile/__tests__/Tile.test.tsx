import React from 'react';
import { render } from '@testing-library/react';
import { Tile } from '../Tile';
import PlusIcon from '@patternfly/react-icons/dist/esm/icons/plus-icon';

describe('Tile', () => {
  test('basic', () => {
    const { asFragment } = render(<Tile title="test" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders selected', () => {
    const { asFragment } = render(<Tile title="test" isSelected />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders disabled', () => {
    const { asFragment } = render(<Tile title="test" isDisabled />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with subtext', () => {
    const { asFragment } = render(<Tile title="test">test subtext</Tile>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with icon', () => {
    const { asFragment } = render(<Tile title="test" icon={<PlusIcon />} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with stacked icon', () => {
    const { asFragment } = render(<Tile title="test" icon={<PlusIcon />} isStacked />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with stacked large icon', () => {
    const { asFragment } = render(<Tile title="test" icon={<PlusIcon />} isStacked isDisplayLarge />);
    expect(asFragment()).toMatchSnapshot();
  });
});
