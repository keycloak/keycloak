/**
 * @vitest-environment jsdom
 */
import { Help } from "@keycloak/keycloak-ui-shared";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { FormProvider, get, useForm } from "react-hook-form";
import { afterEach, describe, expect, it, vi } from "vitest";
import { cacheFieldName, SettingsCache } from "./SettingsCache";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

const Harness = ({
  config,
  onSave,
}: {
  config: Record<string, unknown>;
  onSave: (values: any) => void;
}) => {
  const form = useForm({ defaultValues: { config } });
  return (
    <Help>
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(onSave)}>
          <SettingsCache form={form} unWrap />
          <button type="submit">save</button>
        </form>
      </FormProvider>
    </Help>
  );
};

// The LDAP and Kerberos screens keep the raw ComponentRepresentation, so a config
// entry is an array. The custom provider screen loads through convertToFormValues,
// which unwraps a single-element array, so the same entry is a plain string.
const SHAPES: [string, Record<string, unknown>][] = [
  [
    "array (LDAP, Kerberos)",
    { cachePolicy: ["MAX_LIFESPAN"], maxLifespan: ["50"] },
  ],
  [
    "scalar (custom provider)",
    { cachePolicy: "MAX_LIFESPAN", maxLifespan: "50" },
  ],
];

describe("SettingsCache", () => {
  afterEach(cleanup);

  it.each(SHAPES)(
    "shows the stored maxLifespan for a %s config",
    async (_l, config) => {
      render(<Harness config={config} onSave={vi.fn()} />);

      const input = await screen.findByRole("spinbutton");

      expect((input as HTMLInputElement).value).toBe("50");
    },
  );

  it.each(SHAPES)(
    "saves an edited maxLifespan for a %s config",
    async (_l, config) => {
      const onSave = vi.fn();
      render(<Harness config={config} onSave={onSave} />);
      const input = await screen.findByRole("spinbutton");

      fireEvent.change(input, { target: { value: "120" } });
      fireEvent.click(screen.getByRole("button", { name: "save" }));

      await waitFor(() => expect(onSave).toHaveBeenCalled());
      const saved = onSave.mock.calls[0][0].config.maxLifespan;
      expect(Array.isArray(saved) ? saved[0] : saved).toBe(120);
    },
  );

  it.each(SHAPES)(
    "saves the stored maxLifespan untouched for a %s config",
    async (_l, config) => {
      const onSave = vi.fn();
      render(<Harness config={config} onSave={onSave} />);
      await screen.findByRole("spinbutton");

      fireEvent.click(screen.getByRole("button", { name: "save" }));

      await waitFor(() => expect(onSave).toHaveBeenCalled());
      const saved = onSave.mock.calls[0][0].config.maxLifespan;
      expect(Array.isArray(saved) ? saved[0] : saved).toBe("50");
    },
  );
});

describe("cacheFieldName", () => {
  it("indexes into the array the LDAP and Kerberos screens keep", () => {
    expect(cacheFieldName("config.maxLifespan", ["50"])).toBe(
      "config.maxLifespan[0]",
    );
  });

  it("addresses the scalar the custom provider screen keeps", () => {
    expect(cacheFieldName("config.maxLifespan", "50")).toBe(
      "config.maxLifespan",
    );
  });

  it("keeps the scalar path once a number control has written a number", () => {
    expect(cacheFieldName("config.maxLifespan", 120)).toBe(
      "config.maxLifespan",
    );
  });

  it("keeps the array path while the value is still unset", () => {
    expect(cacheFieldName("config.maxLifespan", undefined)).toBe(
      "config.maxLifespan[0]",
    );
  });

  it("resolves to the stored value in both form shapes", () => {
    const shapes = [
      { maxLifespan: ["50"], evictionHour: ["12"], evictionMinute: ["30"] },
      { maxLifespan: "50", evictionHour: "12", evictionMinute: "30" },
    ];
    const expected = {
      maxLifespan: "50",
      evictionHour: "12",
      evictionMinute: "30",
    };

    for (const config of shapes) {
      for (const [key, want] of Object.entries(expected)) {
        const value = config[key as keyof typeof expected];
        const name = cacheFieldName(`config.${key}`, value);
        expect(get({ config }, name)).toBe(want);
      }
    }
  });
});
