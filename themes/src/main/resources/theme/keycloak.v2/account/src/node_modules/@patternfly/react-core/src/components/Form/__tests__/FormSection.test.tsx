import * as React from 'react';
import { FormSection } from '../FormSection';
import { render, screen } from '@testing-library/react';

test('Check form section example against snapshot', () => {
  const Section = <FormSection />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check form section example with title', () => {
  const Section = <FormSection title="Title" titleElement="h4" />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Verify form section has accessible name when title is passed in', () => {
  render(<FormSection title="Form title" />);

  expect(screen.getByRole('group')).toHaveAccessibleName('Form title');
});

test('Verify form section does not have accessible name when title is not passed in', () => {
  render(<FormSection />);

  expect(screen.getByRole('group')).not.toHaveAccessibleName();
});
