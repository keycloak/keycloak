import { cleanup, render, screen } from "@testing-library/react";
import i18n, { type i18n as I18nInstance } from "i18next";
import type { ReactNode } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { I18nextProvider, initReactI18next } from "react-i18next";
import { afterEach, beforeAll, describe, expect, it, vi } from "vitest";

import { PolicyRow } from "./PolicyRow";

vi.mock("@keycloak/keycloak-ui-shared", async (importOriginal) => ({
  ...(await importOriginal()),
  HelpItem: ({ helpText }: { helpText: string }) => (
    <span data-testid="policy-help-text">{helpText}</span>
  ),
}));

let testI18n: I18nInstance;

afterEach(cleanup);

beforeAll(async () => {
  testI18n = i18n.createInstance();
  await testI18n.use(initReactI18next).init({
    lng: "en",
    resources: {
      en: {
        translation: {
          passwordPoliciesHelp: {
            translatedPolicy: "Translated policy help text.",
          },
        },
      },
    },
  });
});

const TestForm = ({ children }: { children: ReactNode }) => {
  const form = useForm();
  return <FormProvider {...form}>{children}</FormProvider>;
};

const renderPolicy = (id: string, helpText: string) =>
  render(
    <I18nextProvider i18n={testI18n}>
      <TestForm>
        <PolicyRow
          policy={{ id, displayName: id, helpText }}
          onRemove={vi.fn()}
        />
      </TestForm>
    </I18nextProvider>,
  );

describe("PolicyRow help text", () => {
  it("falls back to the provider help text when no translation exists", () => {
    renderPolicy("custom-policy", "Provider help text.");

    expect(screen.getByTestId("policy-help-text").textContent).toBe(
      "Provider help text.",
    );
  });

  it("prefers an existing translation over the provider help text", () => {
    renderPolicy("translatedPolicy", "Provider help text.");

    expect(screen.getByTestId("policy-help-text").textContent).toBe(
      "Translated policy help text.",
    );
  });
});
