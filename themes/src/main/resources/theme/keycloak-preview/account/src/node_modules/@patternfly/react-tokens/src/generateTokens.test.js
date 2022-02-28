/* eslint-disable global-require */
jest
  .mock('fs-extra')
  .mock('glob')
  .mock('fs', () => ({
    ...require.requireActual('fs'),
    readFileSync: jest.fn(() => '')
  }));

let globSyncMock;
let outputFileSyncMock;
let readFileSyncMock;

beforeEach(() => {
  jest.resetModules();
  globSyncMock = require('glob').sync;
  globSyncMock.mockReturnValue(['test.css']);
  readFileSyncMock = require('fs').readFileSync;
  outputFileSyncMock = require('fs-extra').outputFileSync;
});

test('it generates token from root selector with value', () => {
  readFileSyncMock.mockReturnValue(':root { --pf-global--BackgroundColor--100: #fff; }');
  require('./generateTokens');
  expect(getOutputs()).toMatchSnapshot();
});

test('it generates token from class selector with value', () => {
  readFileSyncMock.mockReturnValue('.pf-c-button { --pf-c-button--BackgroundColor: #fff }');
  require('./generateTokens');
  expect(getOutputs()).toMatchSnapshot();
});

test('computes variable value', () => {
  readFileSyncMock.mockReturnValue(
    `.pf-c-button {
      --pf-global--BackgroundColor--100: #fff;
      --pf-c-button--BackgroundColor: var(--pf-global--BackgroundColor--100);
    }`
  );
  require('./generateTokens');
  expect(getOutputs()).toMatchSnapshot();
});

test('keeps variable reference if computing fails', () => {
  readFileSyncMock.mockReturnValue(
    `.pf-c-button {
      --pf-c-button--BackgroundColor: var(--pf-global--BackgroundColor--100);
    }`
  );
  require('./generateTokens');
  expect(getOutputs()).toMatchSnapshot();
});

/**
 *
 */
function getOutputs() {
  return outputFileSyncMock.mock.calls.reduce((acc, call) => {
    const [filePath, content] = call;
    const splitPath = filePath.split(/[/\\]/);
    const name = splitPath.slice(-2).join('/');
    return {
      ...acc,
      [name]: content
    };
  }, {});
}
