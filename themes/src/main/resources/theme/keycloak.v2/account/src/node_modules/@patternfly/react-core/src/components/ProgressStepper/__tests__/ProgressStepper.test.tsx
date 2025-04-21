import * as React from 'react';
import { render } from '@testing-library/react';
import { ProgressStepper } from '../ProgressStepper';
import { ProgressStep } from '../ProgressStep';
import InProgressIcon from '@patternfly/react-icons/dist/esm/icons/in-progress-icon';

describe('ProgressStepper', () => {
  test('renders content', () => {
    const { asFragment } = render(
      <ProgressStepper>
        <ProgressStep>First</ProgressStep>
        <ProgressStep>Second</ProgressStep>
        <ProgressStep>Third</ProgressStep>
      </ProgressStepper>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('gets custom class and id', () => {
    const { asFragment } = render(
      <ProgressStepper className="custom-class" id="test-id">
        <ProgressStep>First</ProgressStep>
        <ProgressStep>Second</ProgressStep>
        <ProgressStep>Third</ProgressStep>
      </ProgressStepper>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders vertically', () => {
    const { asFragment } = render(
      <ProgressStepper isVertical>
        <ProgressStep>First</ProgressStep>
        <ProgressStep>Second</ProgressStep>
        <ProgressStep>Third</ProgressStep>
      </ProgressStepper>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders compact', () => {
    const { asFragment } = render(
      <ProgressStepper isCompact>
        <ProgressStep>First</ProgressStep>
        <ProgressStep>Second</ProgressStep>
        <ProgressStep>Third</ProgressStep>
      </ProgressStepper>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders center aligned', () => {
    const { asFragment } = render(
      <ProgressStepper isCenterAligned>
        <ProgressStep>First</ProgressStep>
        <ProgressStep>Second</ProgressStep>
        <ProgressStep>Third</ProgressStep>
      </ProgressStepper>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});

describe('ProgressStep', () => {
  test('renders content', () => {
    const { asFragment } = render(<ProgressStep>Title</ProgressStep>);
    expect(asFragment()).toMatchSnapshot();
  });

  Object.values(['default', 'success', 'info', 'pending', 'warning', 'danger']).forEach(variant => {
    test(`renders ${variant} variant`, () => {
      const { asFragment } = render(
        <ProgressStep
          variant={variant as 'default' | 'success' | 'info' | 'pending' | 'warning' | 'danger'}
          aria-label={variant}
        >
          {variant} step
        </ProgressStep>
      );
      expect(asFragment()).toMatchSnapshot();
    });
  });

  test('renders current', () => {
    const { asFragment } = render(<ProgressStep isCurrent>Title</ProgressStep>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders help text styling', () => {
    const { asFragment } = render(<ProgressStep popoverRender={() => <div></div>}>Title</ProgressStep>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom icon', () => {
    const { asFragment } = render(<ProgressStep icon={<InProgressIcon />}>Title</ProgressStep>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders custom null icon - removing default from variant', () => {
    const { asFragment } = render(
      <ProgressStep icon={null} variant="success">
        Title
      </ProgressStep>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders description', () => {
    const { asFragment } = render(<ProgressStep description="This is a description">Title</ProgressStep>);
    expect(asFragment()).toMatchSnapshot();
  });
});
