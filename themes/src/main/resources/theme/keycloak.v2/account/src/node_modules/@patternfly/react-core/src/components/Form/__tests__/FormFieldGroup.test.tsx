import * as React from 'react';

import { render, screen } from '@testing-library/react';

import { FormFieldGroup } from '../FormFieldGroup';
import { FormFieldGroupExpandable } from '../FormFieldGroupExpandable';
import { FormFieldGroupHeader } from '../FormFieldGroupHeader';
import { Button } from '../../Button';

test('Check form field group example against snapshot', () => {
  const FieldGroup = (
    <FormFieldGroup
      header={
        <FormFieldGroupHeader
          titleText={{ text: 'Field group 4 (non-expandable)', id: 'title-text-id1' }}
          titleDescription="Field group 4 description text."
          actions={<Button />}
        />
      }
    />
  );
  const { asFragment } = render(FieldGroup);
  expect(asFragment()).toMatchSnapshot();
});

test('Check expandable form field group example against snapshot', () => {
  const FieldGroup = (
    <FormFieldGroupExpandable
      isExpanded
      toggleAriaLabel="toggle"
      header={
        <FormFieldGroupHeader
          titleText={{ text: 'Field group 4 (non-expandable)', id: 'title-text-id2' }}
          titleDescription="Field group 4 description text."
          actions={<Button />}
        />
      }
    />
  );
  const { asFragment } = render(FieldGroup);
  expect(asFragment()).toMatchSnapshot();
});

test('Verify console error logged when there is no aria-label or title', () => {
  const consoleErrorMock = jest.fn();
  global.console = { error: consoleErrorMock } as any;
  const FieldGroup = (
    <FormFieldGroupExpandable
      isExpanded
      header={<FormFieldGroupHeader titleDescription="Field group 4 description text." actions={<Button />} />}
    />
  );
  const { asFragment } = render(FieldGroup);
  expect(consoleErrorMock).toHaveBeenCalled();
});

test('Verify field group has accessible name when header is passed in', () => {
  render(
    <FormFieldGroup
      header={
        <FormFieldGroupHeader
          titleText={{ text: 'Field group 4 (non-expandable)', id: 'title-text-id1' }}
          titleDescription="Field group 4 description text."
          actions={<Button />}
        />
      }
    />
  );

  expect(screen.getByRole('group')).toHaveAccessibleName('Field group 4 (non-expandable)');
});

test('Verify field group does not have accessible name when header is not passed in', () => {
  render(<FormFieldGroup data-testid="field-group-test-id" />);

  expect(screen.getByRole('group')).not.toHaveAccessibleName();
});
