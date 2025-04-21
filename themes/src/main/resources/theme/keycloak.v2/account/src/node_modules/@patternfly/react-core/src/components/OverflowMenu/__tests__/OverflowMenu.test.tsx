import * as React from 'react';

import { render, screen } from '@testing-library/react';

import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenu } from '../OverflowMenu';

describe('OverflowMenu', () => {
  test('md', () => {
    render(<OverflowMenu breakpoint="md" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass(styles.overflowMenu);
  });

  test('lg', () => {
    const { asFragment } = render(<OverflowMenu breakpoint="lg" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('xl', () => {
    const { asFragment } = render(<OverflowMenu breakpoint="xl" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('basic', () => {
    const { asFragment } = render(
      <OverflowMenu breakpoint="md">
        <div>BASIC</div>
      </OverflowMenu>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should warn on bad props', () => {
    const myMock = jest.fn() as any;
    global.console = { error: myMock } as any;

    render(
      <OverflowMenu breakpoint={undefined as 'md'}>
        <div>BASIC</div>
      </OverflowMenu>
    );

    expect(myMock).toHaveBeenCalled();
  });
});
