import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Switch } from '../Switch';

const props = {
  onChange: jest.fn(),
  isChecked: false
};

describe('Switch', () => {
  test('switch label for attribute equals input id attribute', () => {
    render(<Switch id="foo" aria-label="Switch label" />);

    const switchElement = screen.getByLabelText('Switch label');

    expect(switchElement).toHaveAttribute('id', 'foo');
    expect(switchElement.parentElement).toHaveAttribute('for', 'foo');
  });

  test('switch label id is auto generated', () => {
    render(<Switch aria-label="Switch label" />);
    expect(screen.getByLabelText('Switch label')).toHaveAttribute('id');
  });

  test('switch is checked', () => {
    const { asFragment } = render(
      <Switch id="switch-is-checked" label="On" labelOff="Off" isChecked aria-label="Switch label" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('switch is not checked', () => {
    const { asFragment } = render(
      <Switch id="switch-is-not-checked" label="On" labelOff="Off" isChecked={false} aria-label="Switch label" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('switch with only label is checked', () => {
    const { asFragment } = render(<Switch id="switch-is-checked" label="On" isChecked={true} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('switch with only label is not checked', () => {
    const { asFragment } = render(<Switch id="switch-is-not-checked" label="Off" isChecked={false} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('no label switch is checked', () => {
    const { asFragment } = render(<Switch id="no-label-switch-is-checked" isChecked aria-label="Switch label" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('no label switch is not checked', () => {
    const { asFragment } = render(
      <Switch id="no-label-switch-is-not-checked" isChecked={false} aria-label="Switch label" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('switch is checked and disabled', () => {
    const { asFragment } = render(
      <Switch id="switch-is-checked-and-disabled" isChecked isDisabled aria-label="Switch label" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('switch is not checked and disabled', () => {
    const { asFragment } = render(
      <Switch id="switch-is-not-checked-and-disabled" isChecked={false} isDisabled aria-label="Switch label" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('switch passes value and event to onChange handler', () => {
    render(<Switch id="onChange-switch" {...props} aria-label="Switch label" />);

    userEvent.click(screen.getByLabelText('Switch label'));
    expect(props.onChange).toHaveBeenCalledWith(true, expect.any(Object));
  });

  test('should throw console error when no aria-label or label is given', () => {
    const myMock = jest.fn();

    global.console = { ...global.console, error: myMock };

    render(<Switch {...props} />);
    expect(myMock).toHaveBeenCalled();
  });

  test('should not throw console error when label is given but no aria-label', () => {
    const myMock = jest.fn();
    global.console = { ...global.console, error: myMock };

    render(<Switch {...props} label="test switch" />);

    expect(myMock).not.toHaveBeenCalled();
  });

  test('should not throw console error when aria-label is given but no label', () => {
    const myMock = jest.fn();
    global.console = { ...global.console, error: myMock };

    render(<Switch {...props} aria-label="test switch" />);

    expect(myMock).not.toHaveBeenCalled();
  });

  test('should apply reversed modifier', () => {
    render(<Switch id="reversed-switch" label="reversed switch" isReversed aria-label="Switch label" />);
    expect(screen.getByLabelText('Switch label').parentElement).toHaveClass('pf-m-reverse');
  });
});
