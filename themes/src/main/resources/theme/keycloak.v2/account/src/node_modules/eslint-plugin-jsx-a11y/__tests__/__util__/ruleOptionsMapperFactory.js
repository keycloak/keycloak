/**
 * @flow
 */

type ESLintTestRunnerTestCase = {
  code: string,
  errors: ?Array<{ message: string, type: string }>,
  options: ?Array<mixed>,
  parserOptions: ?Array<mixed>,
  settings?: {[string]: mixed},
};

type RuleOptionsMapperFactoryType = (
  params: ESLintTestRunnerTestCase
) => ESLintTestRunnerTestCase;

export default function ruleOptionsMapperFactory(ruleOptions: Array<mixed> = []): RuleOptionsMapperFactoryType {
  // eslint-disable-next-line
  return ({ code, errors, options, parserOptions, settings }: ESLintTestRunnerTestCase): ESLintTestRunnerTestCase => {
    return {
      code,
      errors,
      // Flatten the array of objects in an array of one object.
      options: (options || []).concat(ruleOptions).reduce((acc, item) => [{
        ...acc[0],
        ...item,
      }], [{}]),
      parserOptions,
      settings,
    };
  };
}
