import React from 'react';
import { render, screen } from '@testing-library/react';
import { CardBody } from '../CardBody';

describe('CardBody', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<CardBody />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<CardBody className="extra-class" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });

  test('extra props are spread to the root element', () => {
    const testId = 'card-body';

    render(<CardBody data-testid={testId} />);
    expect(screen.getByTestId(testId)).toBeInTheDocument();
  });

  test('allows passing in a string as the component', () => {
    const component = 'section';

    render(<CardBody component={component}>section content</CardBody>);
    expect(screen.getByText('section content')).toBeInTheDocument();
  });

  test('allows passing in a React Component as the component', () => {
    const Component = () => <div>im a div</div>;

    render(<CardBody component={(Component as unknown) as keyof JSX.IntrinsicElements} />);
    expect(screen.getByText('im a div')).toBeInTheDocument();
  });

  test('body with no-fill applied', () => {
    const { asFragment } = render(<CardBody isFilled={false} />);
    expect(asFragment()).toMatchSnapshot();
  });
});
