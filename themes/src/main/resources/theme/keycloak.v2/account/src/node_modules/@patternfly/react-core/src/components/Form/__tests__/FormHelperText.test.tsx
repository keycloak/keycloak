import React from 'react';

import { render, screen } from '@testing-library/react';

import { ExclamationCircleIcon } from '@patternfly/react-icons';
import { FormHelperText } from '../FormHelperText';

describe('FormHelperText', () => {
  test('renders with PatternFly Core styles', () => {
    const { asFragment } = render(
      <FormHelperText isError isHidden={false}>
        test
      </FormHelperText>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders with icon', () => {
    const { asFragment } = render(
      <FormHelperText isError isHidden={false} icon={<ExclamationCircleIcon />}>
        test
      </FormHelperText>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    const { asFragment } = render(<FormHelperText className="extra-class">test</FormHelperText>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('extra props are spread to the root element', () => {
    const testId = 'login-body';
    render(<FormHelperText data-testid={testId}>test</FormHelperText>);
    expect(screen.getByTestId(testId)).toBeInTheDocument();
  });

  test('LoginFooterItem  with custom node', () => {
    const CustomNode = () => <div>My custom node</div>;

    const { asFragment } = render(
      <FormHelperText>
        <CustomNode />
      </FormHelperText>
    );

    expect(asFragment()).toMatchSnapshot();
  });
});
