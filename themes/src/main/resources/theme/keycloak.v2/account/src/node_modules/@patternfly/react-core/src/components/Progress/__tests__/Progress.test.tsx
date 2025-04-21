import React from 'react';
import { render } from '@testing-library/react';
import { Progress, ProgressSize } from '../Progress';
import { ProgressVariant, ProgressMeasureLocation } from '../ProgressContainer';

test('Simple progress', () => {
  const { asFragment } = render(<Progress value={33} id="progress-simple-example" />);
  expect(asFragment()).toMatchSnapshot();
});

test('no value specified', () => {
  const { asFragment } = render(<Progress id="no-value" />);
  expect(asFragment()).toMatchSnapshot();
});

test('additional label', () => {
  const { asFragment } = render(<Progress id="additional-label" value={33} label="Additional label" />);
  expect(asFragment()).toMatchSnapshot();
});

test('Progress with aria-valuetext', () => {
  const { asFragment } = render(<Progress value={33} id="progress-aria-valuetext" valueText="Descriptive text here" />);
  expect(asFragment()).toMatchSnapshot();
});

test('value lower than minValue', () => {
  const { asFragment } = render(<Progress value={33} id="lower-min-value" min={40} />);
  expect(asFragment()).toMatchSnapshot();
});

test('value higher than maxValue', () => {
  const { asFragment } = render(<Progress value={77} id="higher-max-value" max={60} />);
  expect(asFragment()).toMatchSnapshot();
});

test('value scaled with minValue', () => {
  const { asFragment } = render(<Progress min={10} value={50} id="scaled-min-value" />);
  expect(asFragment()).toMatchSnapshot();
});

test('value scaled with maxValue', () => {
  const { asFragment } = render(<Progress value={50} id="scaled-max-value" max={80} />);
  expect(asFragment()).toMatchSnapshot();
});

test('value scaled between minValue and maxValue', () => {
  const { asFragment } = render(<Progress min={10} value={50} id="scaled-range-value" max={80} />);
  expect(asFragment()).toMatchSnapshot();
});

describe('Progress size', () => {
  Object.keys(ProgressSize).forEach(oneSize => {
    test(oneSize, () => {
      const { asFragment } = render(<Progress id={`${oneSize}-progress`} value={33} size={oneSize as ProgressSize} />);
      expect(asFragment()).toMatchSnapshot();
    });
  });
});

describe('Progress variant', () => {
  Object.keys(ProgressVariant).forEach(oneVariant => {
    test(oneVariant, () => {
      const { asFragment } = render(
        <Progress id={`${oneVariant}-progress`} value={33} variant={oneVariant as ProgressVariant} />
      );
      expect(asFragment()).toMatchSnapshot();
    });
  });
});

describe('Progress measure location', () => {
  Object.keys(ProgressMeasureLocation).forEach(oneLocation => {
    test(oneLocation, () => {
      const { asFragment } = render(
        <Progress id={`${oneLocation}-progress`} value={33} measureLocation={oneLocation as ProgressMeasureLocation} />
      );
      expect(asFragment()).toMatchSnapshot();
    });
  });

  test('inside and small should render large', () => {
    const { asFragment } = render(
      <Progress
        id="large-progress"
        value={33}
        measureLocation={ProgressMeasureLocation.inside}
        size={ProgressSize.sm}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });
});

test('progress component generates console warning when no accessible name is provided', () => {
  const consoleWarnMock = jest.fn();
  global.console = { warn: consoleWarnMock } as any;
  render(<Progress value={33} />);
  expect(consoleWarnMock).toHaveBeenCalled();
});
