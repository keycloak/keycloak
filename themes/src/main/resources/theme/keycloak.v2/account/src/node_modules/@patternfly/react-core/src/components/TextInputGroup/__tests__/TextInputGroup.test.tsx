import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { TextInputGroup, TextInputGroupContext } from '../TextInputGroup';

describe('TextInputGroup', () => {
  it('renders without children', () => {
    render(<TextInputGroup data-testid="TextInputGroup" />);

    expect(screen.getByTestId('TextInputGroup')).toBeVisible();
  });

  it('renders children', () => {
    render(<TextInputGroup>Test</TextInputGroup>);

    expect(screen.getByText('Test')).toBeVisible();
  });

  it('renders with class pf-c-text-input-group', () => {
    render(<TextInputGroup>Test</TextInputGroup>);

    const inputGroup = screen.getByText('Test');

    expect(inputGroup).toHaveClass('pf-c-text-input-group');
  });

  it('renders with custom class names provided via prop', () => {
    render(<TextInputGroup className="custom-class">Test</TextInputGroup>);

    const inputGroup = screen.getByText('Test');

    expect(inputGroup).toHaveClass('custom-class');
  });

  it('does not render with the pf-m-disabled class when not disabled', () => {
    render(<TextInputGroup>Test</TextInputGroup>);

    const inputGroup = screen.getByText('Test');

    expect(inputGroup).not.toHaveClass('pf-m-disabled');
  });

  it('renders with the pf-m-disabled class when disabled', () => {
    render(<TextInputGroup isDisabled>Test</TextInputGroup>);

    const inputGroup = screen.getByText('Test');

    expect(inputGroup).toHaveClass('pf-m-disabled');
  });

  it('passes isDisabled=false to children via a context when isDisabled prop is not passed', () => {
    const TestComponent: React.FunctionComponent = () => {
      const context = React.useContext(TextInputGroupContext);

      return <button disabled={context.isDisabled} />;
    };

    render(
      <TextInputGroup>
        <TestComponent />
      </TextInputGroup>
    );

    const testComponent = screen.getByRole('button');

    expect(testComponent).not.toBeDisabled();
  });

  it('passes isDisabled=true to children via a context when isDisabled prop is passed', () => {
    const TestComponent: React.FunctionComponent = () => {
      const context = React.useContext(TextInputGroupContext);

      return <button disabled={context.isDisabled} />;
    };

    render(
      <TextInputGroup isDisabled>
        <TestComponent />
      </TextInputGroup>
    );

    const testComponent = screen.getByRole('button');

    expect(testComponent).toBeDisabled();
  });

  it('matches the snapshot', () => {
    const { asFragment } = render(<TextInputGroup>Test</TextInputGroup>);

    expect(asFragment()).toMatchSnapshot();
  });
});
