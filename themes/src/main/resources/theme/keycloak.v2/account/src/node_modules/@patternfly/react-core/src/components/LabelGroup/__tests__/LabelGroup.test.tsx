import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Label } from '../../Label';
import { LabelGroup } from '../index';

describe('LabelGroup', () => {
  test('label group default', () => {
    const { asFragment } = render(
      <LabelGroup>
        <Label>1.1</Label>
      </LabelGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('label group with category', () => {
    const { asFragment } = render(
      <LabelGroup categoryName="category">
        <Label>1.1</Label>
      </LabelGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('label group with closable category', () => {
    const { asFragment } = render(
      <LabelGroup categoryName="category" isClosable>
        <Label>1.1</Label>
      </LabelGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('label group expanded', () => {
    render(
      <LabelGroup>
        <Label>1</Label>
        <Label>2</Label>
        <Label>3</Label>
        <Label>4</Label>
      </LabelGroup>
    );
    const showMoreButton = screen.getByRole('button');

    expect(showMoreButton.textContent).toBe('1 more');

    userEvent.click(showMoreButton);
    expect(showMoreButton.textContent).toBe('Show Less');
  });

  test('label group will not render if no children passed', () => {
    render(<LabelGroup data-testid="label-group-test-id" />);
    expect(screen.queryByTestId('label-group-test-id')).toBeNull();
  });

  // TODO, fix test - no tooltip shows up with this categoryName.zzw
  test('label group with category and tooltip', () => {
    const { asFragment } = render(
      <LabelGroup categoryName="A very long category name">
        <Label>1.1</Label>
      </LabelGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('label group compact', () => {
    const { asFragment } = render(
      <LabelGroup isCompact>
        <Label isCompact>1</Label>
        <Label isCompact>2</Label>
        <Label isCompact>3</Label>
        <Label isCompact>4</Label>
      </LabelGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
