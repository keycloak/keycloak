import * as React from 'react';
import { render } from '@testing-library/react';
import { LoginMainHeader } from '../LoginMainHeader';

describe('LoginMainHeader', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<LoginMainHeader />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    const { asFragment } = render(<LoginMainHeader className="extra-class" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('title and subtitle are rendered correctly', () => {
    const { asFragment } = render(<LoginMainHeader title="Log in to your account" subtitle="Use LDAP credentials" />);
    expect(asFragment()).toMatchSnapshot();
  });
});
