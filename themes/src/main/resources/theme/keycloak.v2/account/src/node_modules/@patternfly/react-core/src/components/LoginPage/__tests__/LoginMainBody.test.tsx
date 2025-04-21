import * as React from 'react';
import { render } from '@testing-library/react';
import { LoginMainBody } from '../LoginMainBody';

describe('LoginMainBody', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<LoginMainBody />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    const { asFragment } = render(<LoginMainBody className="extra-class" />);
    expect(asFragment()).toMatchSnapshot();
  });
});
