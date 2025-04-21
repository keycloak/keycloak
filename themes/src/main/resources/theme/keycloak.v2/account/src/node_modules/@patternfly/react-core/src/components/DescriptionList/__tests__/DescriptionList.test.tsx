import React from 'react';
import { render } from '@testing-library/react';
import { DescriptionList } from '../DescriptionList';
import { DescriptionListGroup } from '../DescriptionListGroup';
import { DescriptionListTerm } from '../DescriptionListTerm';
import { DescriptionListDescription } from '../DescriptionListDescription';
import { DescriptionListTermHelpText } from '../DescriptionListTermHelpText';
import { DescriptionListTermHelpTextButton } from '../DescriptionListTermHelpTextButton';

describe('Description List', () => {
  test('default', () => {
    const { asFragment } = render(<DescriptionList />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('1 col on all breakpoints', () => {
    const { asFragment } = render(
      <DescriptionList columnModifier={{ default: '1Col', md: '1Col', lg: '1Col', xl: '1Col', '2xl': '1Col' }} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('2 col on all breakpoints', () => {
    const { asFragment } = render(
      <DescriptionList columnModifier={{ default: '2Col', md: '2Col', lg: '2Col', xl: '2Col', '2xl': '2Col' }} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('3 col on all breakpoints', () => {
    const { asFragment } = render(
      <DescriptionList columnModifier={{ default: '3Col', md: '3Col', lg: '3Col', xl: '3Col', '2xl': '3Col' }} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Horizontal Description List', () => {
    const { asFragment } = render(<DescriptionList isHorizontal />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Compact Description List', () => {
    const { asFragment } = render(<DescriptionList isCompact />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Compact Horizontal Description List', () => {
    const { asFragment } = render(<DescriptionList isCompact isHorizontal />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Fluid Horizontal Description List', () => {
    const { asFragment } = render(<DescriptionList isFluid isHorizontal />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('alignment breakpoints', () => {
    const { asFragment } = render(
      <DescriptionList
        isHorizontal
        orientation={{
          sm: 'horizontal',
          md: 'vertical',
          lg: 'horizontal',
          xl: 'vertical',
          '2xl': 'horizontal'
        }}
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Auto Column Widths Description List', () => {
    const { asFragment } = render(<DescriptionList isAutoColumnWidths />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Inline Grid Description List', () => {
    const { asFragment } = render(<DescriptionList isInlineGrid />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Auto fit Description List', () => {
    const { asFragment } = render(<DescriptionList isAutoFit />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Auto fit with responsive grid Description List', () => {
    const { asFragment } = render(
      <DescriptionList isAutoFit autoFitMinModifier={{ md: '100px', lg: '150px', xl: '200px', '2xl': '300px' }} />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Term default', () => {
    const { asFragment } = render(
      <DescriptionListTerm key="term-id-1" aria-labelledby="term-1">
        test
      </DescriptionListTerm>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Term helper text', () => {
    const { asFragment } = render(
      <DescriptionListTermHelpText key="term-id-1" aria-labelledby="term-1">
        <DescriptionListTermHelpTextButton>test</DescriptionListTermHelpTextButton>
      </DescriptionListTermHelpText>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Group', () => {
    const { asFragment } = render(
      <DescriptionListGroup className="custom-description-list-group" aria-labelledby="group-1">
        test
      </DescriptionListGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Description', () => {
    const { asFragment } = render(
      <DescriptionListDescription className="custom-description-list-description" aria-labelledby="description-1">
        test
      </DescriptionListDescription>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
