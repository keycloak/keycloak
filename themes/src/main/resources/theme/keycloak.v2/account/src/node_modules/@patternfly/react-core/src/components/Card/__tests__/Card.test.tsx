import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { Card, CardContext } from '../Card';

describe('Card', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(<Card />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<Card className="extra-class" />);
    expect(screen.getByRole('article')).toHaveClass('extra-class');
  });

  test('extra props are spread to the root element', () => {
    const testId = 'card';

    render(<Card data-testid={testId} />);
    expect(screen.getByTestId(testId)).toBeInTheDocument();
  });

  test('allows passing in a string as the component', () => {
    const component = 'section';

    render(<Card component={component}>section content</Card>);
    expect(screen.getByText('section content')).toBeInTheDocument();
  });

  test('allows passing in a React Component as the component', () => {
    const Component = () => <div>im a div</div>;

    render(<Card component={(Component as unknown) as keyof JSX.IntrinsicElements} />);
    expect(screen.getByText('im a div')).toBeInTheDocument();
  });

  test('card with isHoverable applied ', () => {
    const { asFragment } = render(<Card isHoverable />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('card with isCompact applied ', () => {
    const { asFragment } = render(<Card isCompact />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('card with isSelectable applied ', () => {
    render(<Card isSelectable />);

    const card = screen.getByRole('article');
    expect(card).toHaveClass('pf-m-selectable');
    expect(card).toHaveAttribute('tabindex', '0');
  });

  test('card with isSelectable and isSelected applied ', () => {
    render(<Card isSelectable isSelected />);

    const card = screen.getByRole('article');
    expect(card).toHaveClass('pf-m-selectable');
    expect(card).toHaveClass('pf-m-selected');
    expect(card).toHaveAttribute('tabindex', '0');
  });

  test('card with only isSelected applied - not change', () => {
    render(<Card isSelected />);

    const card = screen.getByRole('article');
    expect(card).not.toHaveClass('pf-m-selected');
    expect(card.getAttribute('tabindex')).toBeNull();
  });

  test('card with isDisabledRaised applied', () => {
    render(<Card isDisabledRaised />);
    expect(screen.getByRole('article')).toHaveClass('pf-m-non-selectable-raised');
  });

  test('card with isSelectableRaised applied - not change', () => {
    render(<Card isSelectableRaised />);

    const card = screen.getByRole('article');
    expect(card).toHaveClass('pf-m-selectable-raised');
    expect(card).toHaveAttribute('tabindex', '0');
  });

  test('card with isSelectableRaised and isSelected applied ', () => {
    render(<Card isSelected isSelectableRaised />);

    const card = screen.getByRole('article');
    expect(card).toHaveClass('pf-m-selectable-raised');
    expect(card).toHaveClass('pf-m-selected-raised');
    expect(card).toHaveAttribute('tabindex', '0');
  });

  test('card with isFlat applied', () => {
    render(<Card isFlat />);
    expect(screen.getByRole('article')).toHaveClass('pf-m-flat');
  });

  test('card with isExpanded applied', () => {
    render(<Card isExpanded />);
    expect(screen.getByRole('article')).toHaveClass('pf-m-expanded');
  });

  test('card with isRounded applied', () => {
    render(<Card isRounded />);
    expect(screen.getByRole('article')).toHaveClass('pf-m-rounded');
  });

  test('card with isLarge applied', () => {
    render(<Card isLarge />);
    expect(screen.getByRole('article')).toHaveClass('pf-m-display-lg');
  });

  test('card warns when isLarge and isCompact', () => {
    const consoleWarnMock = jest.fn();
    global.console = { warn: consoleWarnMock } as any;

    render(<Card isLarge isCompact />);
    expect(consoleWarnMock).toHaveBeenCalled();
  });

  test('card renders with a hidden input to improve a11y when hasSelectableInput is passed', () => {
    render(<Card isSelectable hasSelectableInput />);

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toBeInTheDocument();
  });

  test('card does not render the hidden input when hasSelectableInput is not passed', () => {
    render(<Card isSelectable />);

    const selectableInput = screen.queryByRole('checkbox', { hidden: true });

    expect(selectableInput).not.toBeInTheDocument();
  });

  test('card warns when hasSelectableInput is passed without selectableInputAriaLabel or a card title', () => {
    const consoleWarnMock = jest.fn();
    global.console = { warn: consoleWarnMock } as any;

    render(<Card isSelectable hasSelectableInput />);

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(consoleWarnMock).toBeCalled();
    expect(selectableInput).toHaveAccessibleName('');
  });

  test('card applies selectableInputAriaLabel to the hidden input', () => {
    render(<Card isSelectable hasSelectableInput selectableInputAriaLabel="Input label test" />);

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toHaveAccessibleName('Input label test');
  });

  test('card applies the supplied card title as the aria label of the hidden input', () => {

    // this component is used to mock the CardTitle's title registry behavior to keep this a pure unit test
    const MockCardTitle = ({ children }) => {
      const { registerTitleId } = React.useContext(CardContext);
      const id = 'card-title-id';

      React.useEffect(() => {
        registerTitleId(id);
      });

      return <div id={id}>{children}</div>;
    };

    render(
      <Card id="card" isSelectable hasSelectableInput>
        <MockCardTitle>Card title from title component</MockCardTitle>
      </Card>
    );

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toHaveAccessibleName('Card title from title component');
  });

  test('card prioritizes selectableInputAriaLabel over card title labelling via card title', () => {

    // this component is used to mock the CardTitle's title registry behavior to keep this a pure unit test
    const MockCardTitle = ({ children }) => {
      const { registerTitleId } = React.useContext(CardContext);
      const id = 'card-title-id';

      React.useEffect(() => {
        registerTitleId(id);
      });

      return <div id={id}>{children}</div>;
    };

    render(
      <Card id="card" isSelectable hasSelectableInput selectableInputAriaLabel="Input label test">
        <MockCardTitle>Card title from title component</MockCardTitle>
      </Card>
    );

    const selectableInput = screen.getByRole('checkbox', { hidden: true });

    expect(selectableInput).toHaveAccessibleName('Input label test');
  });
});
