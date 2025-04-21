import React from 'react';
import { render } from '@testing-library/react';
import { Panel } from '../Panel';
import { PanelMain } from '../PanelMain';
import { PanelMainBody } from '../PanelMainBody';
import { PanelHeader } from '../PanelHeader';
import { PanelFooter } from '../PanelFooter';

describe('Panel', () => {
  test('renders content', () => {
    const { asFragment } = render(<Panel>Foo</Panel>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders content with raised styling', () => {
    const { asFragment } = render(<Panel variant="raised">Foo</Panel>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders content with bordered styling', () => {
    const { asFragment } = render(<Panel variant="bordered">Foo</Panel>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders content with scrollable styling', () => {
    const { asFragment } = render(<Panel isScrollable>Foo</Panel>);
    expect(asFragment()).toMatchSnapshot();
  });
});

describe('PanelMain', () => {
  test('renders content', () => {
    const { asFragment } = render(<PanelMain>Foo</PanelMain>);
    expect(asFragment()).toMatchSnapshot();
  });

  test('renders content with the set maximum height', () => {
    const { asFragment } = render(<PanelMain maxHeight={'80px'}>Foo</PanelMain>);
    expect(asFragment()).toMatchSnapshot();
  });
});

describe('PanelMainBody', () => {
  test('renders content', () => {
    const { asFragment } = render(<PanelMainBody>Foo</PanelMainBody>);
    expect(asFragment()).toMatchSnapshot();
  });
});

describe('PanelHeader', () => {
  test('renders content', () => {
    const { asFragment } = render(<PanelHeader>Foo</PanelHeader>);
    expect(asFragment()).toMatchSnapshot();
  });
});

describe('PanelFooter', () => {
  test('renders content', () => {
    const { asFragment } = render(<PanelFooter>Foo</PanelFooter>);
    expect(asFragment()).toMatchSnapshot();
  });
});
