import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ToggleGroup } from '../ToggleGroup';
import { ToggleGroupItem } from '../ToggleGroupItem';

const props = {
  onChange: jest.fn(),
  selected: false
};

describe('ToggleGroup', () => {
  test('basic selected', () => {
    const { asFragment } = render(
      <ToggleGroupItem text="test" isSelected buttonId="toggleGroupItem" aria-label="basic selected" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('basic not selected', () => {
    const { asFragment } = render(
      <ToggleGroupItem text="test" buttonId="toggleGroupItem" aria-label="basic not selected" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('icon variant', () => {
    const { asFragment } = render(
      <ToggleGroupItem isSelected icon="icon" buttonId="toggleGroupItem" aria-label="icon variant" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isDisabled', () => {
    const { asFragment } = render(
      <ToggleGroupItem text="test" isDisabled buttonId="toggleGroupItem" aria-label="isDisabled" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('item passes selection and event to onChange handler', () => {
    render(
      <ToggleGroupItem text="test" buttonId="toggleGroupItem" onChange={props.onChange} aria-label="onChange handler" />
    );

    userEvent.click(screen.getByRole('button'));
    expect(props.onChange).toHaveBeenCalledWith(true, expect.any(Object));
  });

  test('isCompact', () => {
    const { asFragment } = render(
      <ToggleGroup isCompact aria-label="Label">
        <ToggleGroupItem text="Test" />
        <ToggleGroupItem text="Test" />
      </ToggleGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
