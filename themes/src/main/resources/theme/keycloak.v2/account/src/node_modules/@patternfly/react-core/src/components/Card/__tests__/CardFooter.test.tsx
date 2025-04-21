import React from 'react';
import { render, screen } from '@testing-library/react';
import { CardFooter } from '../CardFooter';

describe('CardFooter', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<CardFooter />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<CardFooter className="extra-class">text</CardFooter>);
    expect(screen.getByText('text')).toHaveClass('extra-class');
  });

  test('extra props are spread to the root element', () => {
    const testId = 'card-footer';

    render(<CardFooter data-testid={testId} />);
    expect(screen.getByTestId(testId)).toBeInTheDocument();
  });

  test('allows passing in a string as the component', () => {
    render(<CardFooter component={'div'}>div content</CardFooter>);
    expect(screen.getByText('div content')).toBeInTheDocument();
  });

  test('allows passing in a React Component as the component', () => {
    const Component = () => <div>im a div</div>;
    render(<CardFooter component={(Component as unknown) as keyof JSX.IntrinsicElements} />);
    expect(screen.getByText('im a div')).toBeInTheDocument();
  });
});
