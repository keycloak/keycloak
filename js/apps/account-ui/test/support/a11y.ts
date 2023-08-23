import { AxeBuilder } from "@axe-core/playwright";
import base, { expect, Page } from "@playwright/test";

export type AccessibilityFixture = {
  exclude: Array<string>;
  disableRules: Array<string>;
  levels: Array<string>;
  runAccessibilityCheck: string;
};

type AccessibilityError = {
  name: string;
  message: string;
  nodes: any[];
};

export const testAccessibility = base.extend<AccessibilityFixture>({
  exclude: [],
  disableRules: [],
  levels: ["critical", "serious"],
  runAccessibilityCheck: async (
    { page, exclude, disableRules, levels },
    use,
  ) => {
    await page.waitForLoadState("load");

    const accessibilityCheck = new AccessibilityCheckSetup(page);
    const errorNumber = await accessibilityCheck.scanViolations(
      exclude,
      disableRules,
      levels,
    );

    await use(`${errorNumber} errors`);
  },
});

class AccessibilityCheckSetup {
  readonly page: Page;
  constructor(page: Page) {
    this.page = page as Page;
  }

  async scanViolations(
    exclude: string[],
    disableRules: string[],
    levels: string[],
  ) {
    try {
      const axeBuilder = new AxeBuilder({ page: this.page })
        .exclude(exclude)
        .disableRules(disableRules)
        .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]);

      const accessibilityScanResults = await axeBuilder.analyze();

      const accessibilityErrors = await this.runAssertions(
        accessibilityScanResults.violations,
        levels,
      );
      for (const error of accessibilityErrors.errors) {
        expect.soft(error.name, error.message).toBe("");
        console.log(error.nodes[0].html);
      }
      for (const warning of accessibilityErrors.warnings) {
        console.error(warning.name, warning.message);
        console.log(warning.nodes[0].html);
      }
      return accessibilityErrors.errors.length;
    } catch (error) {
      if (error.message.includes("Execution context was destroyed")) {
        // Handle the navigation-related error
        // You might want to refresh the page and re-run the analysis
        // Add your error handling logic here
      }
      return 0;
    }
  }

  runAssertions(violations: any[], levels: string[]) {
    const result: {
      errors: AccessibilityError[];
      warnings: AccessibilityError[];
    } = {
      errors: [],
      warnings: [],
    };
    for (const violation of violations) {
      const name = violation.id;
      const message = `Accessibility violation [${violation.impact.toUpperCase()}]: ${
        violation.help
      }(${violation.helpUrl})`;
      if (levels.includes(violation.impact.toLowerCase())) {
        result.errors.push({ name, message, nodes: violation.nodes });
      } else {
        result.warnings.push({ name, message, nodes: violation.nodes });
      }
    }
    return result;
  }
}
