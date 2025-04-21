import React from 'react';
import { render, screen } from '@testing-library/react';
import { Bullseye } from '../Bullseye';

describe('Bullseye', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<Bullseye />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<Bullseye className="extra-class" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });

  test('allows passing in a string as the component', () => {
    const component = 'button';

    render(<Bullseye component={component} />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  test('allows passing in a React Component as the component', () => {
    const Component: React.FunctionComponent = () => <div>Some text</div>;

    render(<Bullseye component={(Component as unknown) as keyof JSX.IntrinsicElements} />);
    expect(screen.getByText('Some text')).toBeInTheDocument();
  });
});
