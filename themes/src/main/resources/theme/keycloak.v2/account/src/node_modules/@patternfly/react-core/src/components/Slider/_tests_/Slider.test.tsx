import React from 'react';
import { render, screen } from '@testing-library/react';
import { Slider } from '../Slider';
import { Button } from '../../Button';

describe('slider', () => {
  test('renders continuous slider', () => {
    const { asFragment } = render(<Slider value={50} isInputVisible inputValue={50} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders discrete slider', () => {
    const { asFragment } = render(<Slider value={50} min={10} max={110} step={2} isInputVisible inputValue={50} />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders discrete slider with custom steps', () => {
    const { asFragment } = render(
      <Slider
        value={50}
        customSteps={[
          { value: 0, label: '0%' },
          { value: 25, label: '25%', isLabelHidden: true },
          { value: 50, label: '50%' },
          { value: 75, label: '75%', isLabelHidden: true },
          { value: 100, label: '100%' }
        ]}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders continuous slider with custom steps', () => {
    const { asFragment } = render(
      <Slider
        value={50}
        areCustomStepsContinuous
        customSteps={[
          { value: 0, label: '0%' },
          { value: 100, label: '100%' }
        ]}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders slider with input', () => {
    const { asFragment } = render(
      <Slider value={50} isInputVisible inputValue={50} inputLabel="%" inputPosition="right" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders slider with input above thumb', () => {
    const { asFragment } = render(
      <Slider value={50} isInputVisible inputValue={50} inputLabel="%" inputPosition="aboveThumb" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders slider with input actions', () => {
    const { asFragment } = render(
      <Slider
        value={50}
        leftActions={<Button variant="plain" aria-label="Minus" />}
        rightActions={<Button variant="plain" aria-label="Plus" />}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders disabled slider', () => {
    const { asFragment } = render(<Slider value={50} isDisabled />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders slider with tooltip on thumb', () => {
    const { asFragment } = render(<Slider value={50} hasTooltipOverThumb />);
    expect(asFragment()).toMatchSnapshot();
  });
});

test('renders slider with aria-labelledby', () => {
  render(
    <>
      <p id="test">label text</p>
      <Slider value={50} aria-labelledby="test" />
    </>
  );

  const slider = screen.getByRole('slider', { name: 'label text' });

  expect(slider).toBeVisible();
});

test('renders slider with aria-describedby', () => {
  render(
    <>
      <p id="test">descriptive text about the slider</p>
      <Slider value={50} aria-describedby="test" />
    </>
  );

  const slider = screen.getByRole('slider', { description: 'descriptive text about the slider' });

  expect(slider).toBeVisible();
});
