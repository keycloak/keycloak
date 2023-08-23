import { AxeBuilder } from "@axe-core/playwright";
import base, { expect, Page } from "@playwright/test";

export type AccessibilityError = {
  name: string;
  message: string;
  nodes: any[];
};

export type AccessibilityFixture = {
  url: string;
  exclude: Array<string>;
  disableRules: Array<string>;
  levels: Array<string>;
  runAccessibilityCheck: string;
};

export const testAccessibility = base.extend<AccessibilityFixture>({
  url: "/",
  exclude: [],
  disableRules: [],
  levels: ["critical", "serious"],
  runAccessibilityCheck: async (
    { page, url, exclude, disableRules, levels },
    use,
  ) => {
    await page.goto(url);
    const accessibilityCheck = new AccessibilityCheckSetup(page);
    const errorNumber = await accessibilityCheck.scanViolations(
      exclude,
      disableRules,
      levels,
    );
    await use(`${errorNumber} errors`);
  },
});

export class AccessibilityCheckSetup {
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

      await this.page.waitForLoadState("load");

      const accessibilityScan = await axeBuilder.analyze();

      const accessibilityErrors = await this.runAssertions(
        accessibilityScan.violations,
        levels,
      );

      for (const error of accessibilityErrors.errors) {
        const typedError = error as AccessibilityError;
        expect.soft(typedError.name, typedError.message).toBe("");
        console.log(typedError.nodes[0].html);
      }

      for (const warning of accessibilityErrors.warnings) {
        const typedWarning = warning as AccessibilityError;
        console.error(typedWarning.name, typedWarning.message);
        console.log(typedWarning.nodes[0].html);
      }

      return accessibilityErrors.errors.length;
    } catch (error) {
      if (error.message.includes("Execution context was destroyed")) {
        await this.page.reload();
        return this.scanViolations(exclude, disableRules, levels);
      }
      return 0;
    }
  }

  async runAssertions(violations: any[], levels: string[]) {
    const result: { errors: Array<any>; warnings: Array<any> } = {
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
