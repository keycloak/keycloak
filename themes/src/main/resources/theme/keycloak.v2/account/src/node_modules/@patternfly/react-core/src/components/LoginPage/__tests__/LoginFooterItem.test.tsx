import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { LoginFooterItem } from '../LoginFooterItem';

describe('LoginFooterItem', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<LoginFooterItem target="_self" href="#" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<LoginFooterItem className="extra-class" />);
    expect(screen.getByRole('link')).toHaveClass('extra-class');
  });

  test('with custom node', () => {
    const CustomNode = () => <div>My custom node</div>;

    render(
      <LoginFooterItem>
        <CustomNode />
      </LoginFooterItem>
    );

    expect(screen.getByText('My custom node')).toBeInTheDocument();
  });
});
