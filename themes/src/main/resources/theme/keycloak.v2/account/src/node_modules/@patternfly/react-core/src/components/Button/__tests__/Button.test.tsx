import React from 'react';

import { render, screen } from '@testing-library/react';

import CartArrowDownIcon from '@patternfly/react-icons/dist/esm/icons/cart-arrow-down-icon';
import { Button, ButtonVariant } from '../Button';

describe('Button', () => {
  Object.values(ButtonVariant).forEach(variant => {
    test(`${variant} button`, () => {
      const { asFragment } = render(
        <Button variant={variant} aria-label={variant}>
          {variant} Button
        </Button>
      );
      expect(asFragment()).toMatchSnapshot();
    });
  });

  test('it adds an aria-label to plain buttons', () => {
    const label = 'aria-label test';
    render(<Button aria-label={label} />);

    expect(screen.getByLabelText(label)).toBeTruthy();
  });

  test('link with icon', () => {
    const { asFragment } = render(
      <Button variant={ButtonVariant.link} icon={<CartArrowDownIcon />}>
        Block Button
      </Button>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isBlock', () => {
    const { asFragment } = render(<Button isBlock>Block Button</Button>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isDisabled', () => {
    const { asFragment } = render(<Button isDisabled>Disabled Button</Button>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isDanger secondary', () => {
    const { asFragment } = render(
      <Button variant="secondary" isDanger>
        Disabled Button
      </Button>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isDanger link', () => {
    const { asFragment } = render(
      <Button variant="link" isDanger>
        Disabled Button
      </Button>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isAriaDisabled button', () => {
    const { asFragment } = render(<Button isAriaDisabled>Disabled yet focusable button</Button>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isAriaDisabled link button', () => {
    const { asFragment } = render(
      <Button isAriaDisabled component="a">
        Disabled yet focusable button
      </Button>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isInline', () => {
    const { asFragment } = render(
      <Button variant={ButtonVariant.link} isInline>
        Hovered Button
      </Button>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isSmall', () => {
    const { asFragment } = render(<Button isSmall>Small Button</Button>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isLarge', () => {
    const { asFragment } = render(<Button isLarge>Large Button</Button>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isLoading', () => {
    const { asFragment } = render(
      <Button isLoading spinnerAriaValueText="Loading">
        Loading Button
      </Button>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('allows passing in a string as the component', () => {
    const component = 'a';
    render(<Button component={component}>anchor button</Button>);

    expect(screen.getByText('anchor button')).toBeInTheDocument();
  });

  test('allows passing in a React Component as the component', () => {
    const Component = () => <div>im a div</div>;
    render(<Button component={Component} />);

    expect(screen.getByText('im a div')).toBeInTheDocument();
  });

  test('aria-disabled is set to true and tabIndex to -1 if component is not a button and is disabled', () => {
    const { asFragment } = render(
      <Button component="a" isDisabled>
        Disabled Anchor Button
      </Button>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('setting tab index through props', () => {
    render(<Button tabIndex={0}>TabIndex 0 Button</Button>);
    expect(screen.getByRole('button')).toHaveAttribute('tabindex', '0');
  });

  test('isLoading icon only', () => {
    const { asFragment } = render(
      <Button variant="plain" isLoading aria-label="Upload" spinnerAriaValueText="Loading" icon={<div>ICON</div>} />
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
