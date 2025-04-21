import * as React from 'react';
import { render } from '@testing-library/react';
import { LoginMainFooterLinksItem } from '../LoginMainFooterLinksItem';

describe('LoginMainFooterLinksItem', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<LoginMainFooterLinksItem href="#" target="" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    const { asFragment } = render(<LoginMainFooterLinksItem className="extra-class" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('with custom node', () => {
    const CustomNode = () => <div>My custom node</div>;

    const { asFragment } = render(
      <LoginMainFooterLinksItem>
        <CustomNode />
      </LoginMainFooterLinksItem>
    );

    expect(asFragment()).toMatchSnapshot();
  });
});
