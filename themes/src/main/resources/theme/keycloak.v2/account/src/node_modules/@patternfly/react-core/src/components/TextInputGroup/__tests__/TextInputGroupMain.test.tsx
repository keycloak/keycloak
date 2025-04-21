import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';

import { TextInputGroupMain } from '../TextInputGroupMain';
import { TextInputGroupContext } from '../TextInputGroup';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';

describe('TextInputGroupMain', () => {
  it('renders without children', () => {
    render(<TextInputGroupMain data-testid="TextInputGroupMain" />);

    expect(screen.getByTestId('TextInputGroupMain')).toBeVisible();
  });

  it('renders children', () => {
    render(<TextInputGroupMain>Test</TextInputGroupMain>);

    expect(screen.getByText('Test')).toBeVisible();
  });

  it('renders with class pf-c-text-input-group__main', () => {
    render(<TextInputGroupMain>Test</TextInputGroupMain>);

    const inputGroupMain = screen.getByText('Test');

    expect(inputGroupMain).toHaveClass('pf-c-text-input-group__main');
  });

  it('does not render with class pf-m-icon when an icon prop is not passed', () => {
    render(<TextInputGroupMain>Test</TextInputGroupMain>);

    const inputGroupMain = screen.getByText('Test');

    expect(inputGroupMain).not.toHaveClass('pf-m-icon');
  });

  it('renders with class pf-m-icon when an icon prop is passed', () => {
    render(<TextInputGroupMain icon="icon">Test</TextInputGroupMain>);

    const inputGroupMain = screen.getByText('Test');

    expect(inputGroupMain).toHaveClass('pf-m-icon');
  });

  it('renders with custom class names provided via prop', () => {
    render(<TextInputGroupMain className="custom-class">Test</TextInputGroupMain>);

    const inputGroupMain = screen.getByText('Test');

    expect(inputGroupMain).toHaveClass('custom-class');
  });

  it('renders with class pf-c-text-input-group__text on the inputs parent', () => {
    render(<TextInputGroupMain>Test</TextInputGroupMain>);

    const inputGroupText = screen.getByRole('textbox').parentNode;

    expect(inputGroupText).toHaveClass('pf-c-text-input-group__text');
  });

  it('renders the input with class pf-c-text-input-group__text-input', () => {
    render(<TextInputGroupMain>Test</TextInputGroupMain>);

    const input = screen.getByRole('textbox');

    expect(input).toHaveClass('pf-c-text-input-group__text-input');
  });

  it('does not render the icon when it is not passed', () => {
    render(<TextInputGroupMain />);

    const icon = screen.queryByRole('img', { hidden: true });

    expect(icon).not.toBeInTheDocument();
  });

  it('renders the icon when passed', () => {
    render(<TextInputGroupMain icon={<SearchIcon />} />);

    const icon = screen.getByRole('img', { hidden: true });

    expect(icon).toBeInTheDocument();
  });

  it('renders the icon as aria hidden', () => {
    render(<TextInputGroupMain icon={<SearchIcon />} />);

    const icon = screen.getByRole('img', { hidden: true });

    expect(icon).toHaveAttribute('aria-hidden', 'true');
  });

  it('passes the aria-label prop to the input', () => {
    render(<TextInputGroupMain aria-label="Test label">Test</TextInputGroupMain>);

    const input = screen.getByRole('textbox');

    expect(input).toHaveAccessibleName('Test label');
  });

  it('passes the value prop to the input', () => {
    render(<TextInputGroupMain value="value text">Test</TextInputGroupMain>);

    const inputValue = screen.getByDisplayValue('value text');

    expect(inputValue).toBeVisible();
  });

  it('passes the placeholder prop to the input', () => {
    render(<TextInputGroupMain placeholder="placeholder text">Test</TextInputGroupMain>);

    const inputPlaceholder = screen.getByPlaceholderText('placeholder text');

    expect(inputPlaceholder).toBeVisible();
  });

  it('defaults to an input type of text', () => {
    render(<TextInputGroupMain>Test</TextInputGroupMain>);

    const input = screen.getByRole('textbox');

    expect(input).toHaveAttribute('type', 'text');
  });

  it('updates the input type when a different type is passed', () => {
    render(<TextInputGroupMain type="search">Test</TextInputGroupMain>);

    const textInput = screen.queryByRole('textbox');
    const searchInput = screen.getByRole('searchbox');

    expect(textInput).not.toBeInTheDocument();
    expect(searchInput).toBeVisible();
  });

  it('does not disable the input when TextInputGroupContext isDisabled is false', () => {
    render(
      <TextInputGroupContext.Provider value={{ isDisabled: false }}>
        <TextInputGroupMain />
      </TextInputGroupContext.Provider>
    );

    const input = screen.getByRole('textbox');

    expect(input).not.toBeDisabled();
  });

  it('disables the input when TextInputGroupContext isDisabled is true', () => {
    render(
      <TextInputGroupContext.Provider value={{ isDisabled: true }}>
        <TextInputGroupMain />
      </TextInputGroupContext.Provider>
    );

    const input = screen.getByRole('textbox');

    expect(input).toBeDisabled();
  });

  it("doesn't render the hint input when a hint prop isn't passed", () => {
    // we set the type of the main input to search here so that we can accurately target the hint input
    render(<TextInputGroupMain type="search">Test</TextInputGroupMain>);

    const hintInput = screen.queryByRole('textbox', { hidden: true });

    expect(hintInput).not.toBeInTheDocument();
  });

  it('renders the hint input when a hint prop is passed', () => {
    // we set the type of the main input to search here so that we can accurately target the hint input
    render(
      <TextInputGroupMain hint="Test" type="search">
        Test
      </TextInputGroupMain>
    );

    const hintInput = screen.getByRole('textbox', { hidden: true });

    expect(hintInput).toBeInTheDocument();
  });

  it('renders the hint input with classes pf-c-text-input-group__text-input and pf-m-hint', () => {
    // we set the type of the main input to search here so that we can accurately target the hint input
    render(
      <TextInputGroupMain hint="Test" type="search">
        Test
      </TextInputGroupMain>
    );

    const hintInput = screen.getByRole('textbox', { hidden: true });

    expect(hintInput).toHaveClass('pf-m-hint');
    expect(hintInput).toHaveClass('pf-c-text-input-group__text-input');
  });

  it('renders the hint input as disabled', () => {
    // we set the type of the main input to search here so that we can accurately target the hint input
    render(
      <TextInputGroupMain hint="Test" type="search">
        Test
      </TextInputGroupMain>
    );

    const hintInput = screen.getByRole('textbox', { hidden: true });

    expect(hintInput).toBeDisabled();
  });

  it('renders the hint input as aria-hidden', () => {
    // we set the type of the main input to search here so that we can accurately target the hint input
    render(
      <TextInputGroupMain hint="Test" type="search">
        Test
      </TextInputGroupMain>
    );

    const hintInput = screen.getByRole('textbox', { hidden: true });

    expect(hintInput).toHaveAttribute('aria-hidden', 'true');
  });

  it('passes the hint prop to the hint input as its value', () => {
    // we set the type of the main input to search here so that we can accurately target the hint input
    render(
      <TextInputGroupMain hint="value text" type="search">
        Test
      </TextInputGroupMain>
    );

    const hintInput = screen.getByDisplayValue('value text');

    expect(hintInput).toBeVisible();
  });

  it('does not call onChange callback when the input does not change', () => {
    const onChangeMock = jest.fn();

    render(<TextInputGroupMain onChange={onChangeMock}>Test</TextInputGroupMain>);

    expect(onChangeMock).not.toHaveBeenCalled();
  });

  it('calls the onChange callback when the input changes', () => {
    const onChangeMock = jest.fn();

    render(<TextInputGroupMain onChange={onChangeMock}>Test</TextInputGroupMain>);

    const input = screen.getByRole('textbox');
    userEvent.type(input, 'Test');

    expect(onChangeMock).toHaveBeenCalledTimes(4);
  });

  it('does not call onFocus callback when the input does not get focus', () => {
    const onFocusMock = jest.fn();

    render(<TextInputGroupMain onFocus={onFocusMock}>Test</TextInputGroupMain>);

    expect(onFocusMock).not.toHaveBeenCalled();
  });

  it('calls the onFocus callback when the input is focused', () => {
    const onFocusMock = jest.fn();

    render(<TextInputGroupMain onFocus={onFocusMock}>Test</TextInputGroupMain>);

    const input = screen.getByRole('textbox');
    userEvent.click(input);

    expect(onFocusMock).toHaveBeenCalledTimes(1);
  });

  it('does not call onBlur callback when the input does not lose focus', () => {
    const onBlurMock = jest.fn();

    render(<TextInputGroupMain onBlur={onBlurMock}>Test</TextInputGroupMain>);

    const input = screen.getByRole('textbox');
    userEvent.click(input);

    expect(onBlurMock).not.toHaveBeenCalled();
  });

  it('calls the onBlur callback when the input loses focus', () => {
    const onBlurMock = jest.fn();

    render(<TextInputGroupMain onBlur={onBlurMock}>Test</TextInputGroupMain>);

    const input = screen.getByRole('textbox');
    userEvent.click(input);
    userEvent.click(document.body);

    expect(onBlurMock).toHaveBeenCalledTimes(1);
  });

  it('matches the snapshot', () => {
    const { asFragment } = render(<TextInputGroupMain>Test</TextInputGroupMain>);

    expect(asFragment()).toMatchSnapshot();
  });
});
