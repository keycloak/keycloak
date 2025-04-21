import React from 'react';
import { render } from '@testing-library/react';
import { CodeBlock } from '../CodeBlock';
import { CodeBlockAction } from '../CodeBlockAction';
import { CodeBlockCode } from '../CodeBlockCode';

test('CodeBlock renders successfully', () => {
  const { asFragment } = render(<CodeBlock>test text</CodeBlock>);
  expect(asFragment()).toMatchSnapshot();
});

test('CodeBlockAction renders successfully', () => {
  const { asFragment } = render(<CodeBlockAction>action</CodeBlockAction>);
  expect(asFragment()).toMatchSnapshot();
});

test('CodeBlockCode renders successfully', () => {
  const { asFragment } = render(<CodeBlockCode>action</CodeBlockCode>);
  expect(asFragment()).toMatchSnapshot();
});

test('CodeBlock with components renders successfully', () => {
  const { asFragment } = render(
    <CodeBlock actions={<CodeBlockAction>button</CodeBlockAction>}>
      <CodeBlockCode>inside pre/code tags</CodeBlockCode>
      test outer text
    </CodeBlock>
  );
  expect(asFragment()).toMatchSnapshot();
});
