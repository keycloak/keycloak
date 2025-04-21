import React from 'react';
import { render } from '@testing-library/react';
import { MenuToggle } from '../MenuToggle';
import { Badge } from '../../Badge';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import EllipsisVIcon from '@patternfly/react-icons/dist/esm/icons/ellipsis-v-icon';

describe('menu toggle', () => {
  test('renders successfully', () => {
    const { asFragment } = render(<MenuToggle>Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passes additional classes', () => {
    const { asFragment } = render(<MenuToggle className="test-class">Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('shows isExpanded', () => {
    const { asFragment } = render(<MenuToggle isExpanded>Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('shows isDisabled', () => {
    const { asFragment } = render(<MenuToggle isDisabled>Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('shows isPrimary', () => {
    const { asFragment } = render(<MenuToggle variant="primary">Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('shows isPlain', () => {
    const { asFragment } = render(
      <MenuToggle variant="plain">
        <EllipsisVIcon />
      </MenuToggle>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('shows plain text', () => {
    const { asFragment } = render(<MenuToggle variant="plainText">Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('shows icon', () => {
    const { asFragment } = render(<MenuToggle icon={<CogIcon />}>Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('shows badge', () => {
    const { asFragment } = render(<MenuToggle badge={<Badge>badge</Badge>}>Toggle</MenuToggle>);
    expect(asFragment()).toMatchSnapshot();
  });
});
