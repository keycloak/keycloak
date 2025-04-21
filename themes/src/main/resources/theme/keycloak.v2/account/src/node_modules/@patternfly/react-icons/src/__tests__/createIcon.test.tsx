import React from 'react';
import { render, screen } from '@testing-library/react';
import { createIcon, IconSize } from '../createIcon';

const iconDef = {
  name: 'IconName',
  width: 10,
  height: 20,
  svgPath: 'svgPath'
};

const SVGIcon = createIcon(iconDef);

test('sets correct viewBox', () => {
  render(<SVGIcon />);
  expect(screen.getByRole('img', { hidden: true })).toHaveAttribute(
    'viewBox',
    `0 0 ${iconDef.width} ${iconDef.height}`
  );
});

test('sets correct svgPath', () => {
  render(<SVGIcon />);
  expect(
    screen
      .getByRole('img', { hidden: true })
      .querySelector('path')
  ).toHaveAttribute('d', iconDef.svgPath);
});

test('height and width are set from size', () => {
  render(<SVGIcon size={IconSize.sm} />);

  const svg = screen.getByRole('img', { hidden: true });
  expect(svg).toHaveAttribute('width', '1em');
  expect(svg).toHaveAttribute('height', '1em');
});

test('aria-hidden is true if no title is specified', () => {
  render(<SVGIcon />);
  expect(screen.getByRole('img', { hidden: true })).toHaveAttribute('aria-hidden', 'true');
});

test('title is not renderd if a title is not passed', () => {
  render(<SVGIcon />);
  expect(screen.getByRole('img', { hidden: true }).querySelector('title')).toBeNull();
});

test('aria-labelledby is null if a title is not passed', () => {
  render(<SVGIcon />);
  expect(screen.getByRole('img', { hidden: true })).not.toHaveAttribute('aria-labelledby');
});

test('title is rendered', () => {
  const title = 'icon title';

  render(<SVGIcon title={title} />);
  expect(screen.getByText(title)).toBeInTheDocument();
});

test('aria-labelledby matches title id', () => {
  render(<SVGIcon title="icon title" />);

  const svg = screen.getByRole('img', { hidden: true });
  const labelledby = svg.getAttribute('aria-labelledby');
  const titleId = svg.querySelector('title').getAttribute('id');

  expect(labelledby).toEqual(titleId);
});

test('additional props should be spread to the root svg element', () => {
  render(<SVGIcon data-testid="icon" />);
  expect(screen.getByTestId('icon')).toBeInTheDocument();
});
