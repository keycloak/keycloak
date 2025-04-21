import React from 'react';
import { render } from '@testing-library/react';
import { NumberInput } from '../NumberInput';

describe('numberInput', () => {
  test('renders defaults & extra props', () => {
    const { asFragment } = render(<NumberInput className="custom" id="numberInput1" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders value', () => {
    const { asFragment } = render(<NumberInput value={90} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders disabled', () => {
    const { asFragment } = render(<NumberInput value={90} isDisabled />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('disables lower threshold', () => {
    const { asFragment } = render(<NumberInput value={0} min={0} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('disables upper threshold', () => {
    const { asFragment } = render(<NumberInput value={100} max={100} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders unit', () => {
    const { asFragment } = render(<NumberInput value={5} unit="%" unitPosition="after" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders unit & position', () => {
    const { asFragment } = render(<NumberInput value={5} unit="$" unitPosition="before" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom width', () => {
    const { asFragment } = render(<NumberInput value={5} widthChars={10} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passes input props successfully', () => {
    const { asFragment } = render(<NumberInput value={5} onChange={jest.fn()} inputName="test-name" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passes button props successfully', () => {
    const { asFragment } = render(
      <NumberInput
        value={5}
        onMinus={jest.fn()}
        minusBtnProps={{ id: 'minus-id' }}
        onPlus={jest.fn()}
        plusBtnProps={{ id: 'plus-id' }}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
