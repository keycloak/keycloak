/**
 * @vitest-environment jsdom
 */
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { Help } from "@keycloak/keycloak-ui-shared";
import {
  act,
  cleanup,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import i18next from "i18next";
import { useEffect, useState } from "react";
import { initReactI18next } from "react-i18next";
import { FormProvider, UseFormReturn, useForm } from "react-hook-form";
import {
  afterAll,
  afterEach,
  beforeAll,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { convertToFormValues } from "../../util";
import { SettingsCache, setupCacheForm } from "./SettingsCache";

const component: ComponentRepresentation = {
  id: "8d1e1b0a-3e1e-4f4e-9b0a-1b0a3e1e4f4e",
  name: "my-provider",
  config: {
    cachePolicy: ["MAX_LIFESPAN"],
    evictionDay: ["4"],
    evictionHour: ["12"],
    evictionMinute: ["30"],
    maxLifespan: ["50"],
    someProviderProperty: ["a value"],
  },
};

/**
 * How the custom provider screen populates the form: the conversion needed by
 * the provider's own properties, followed by the cache entries being restored.
 */
const customProviderLoad = (form: UseFormReturn<ComponentRepresentation>) => {
  convertToFormValues(component, form.setValue);
  setupCacheForm(component, form.setValue);
};

/** How the LDAP and Kerberos screens populate the form. */
const ldapLoad = (form: UseFormReturn<ComponentRepresentation>) =>
  form.reset(structuredClone(component));

const LOAD_PATHS: [
  string,
  (form: UseFormReturn<ComponentRepresentation>) => void,
][] = [
  ["custom provider screen", customProviderLoad],
  ["LDAP and Kerberos screens", ldapLoad],
];

describe("setupCacheForm", () => {
  it("keeps the values addressable by the index the cache fields use", () => {
    const { result } = renderHook(() => useForm<ComponentRepresentation>());

    act(() => customProviderLoad(result.current));

    const { getValues } = result.current;
    expect(getValues("config.cachePolicy[0]")).toBe("MAX_LIFESPAN");
    expect(getValues("config.evictionDay[0]")).toBe("4");
    expect(getValues("config.evictionHour[0]")).toBe("12");
    expect(getValues("config.evictionMinute[0]")).toBe("30");
    expect(getValues("config.maxLifespan[0]")).toBe("50");
  });

  it("produces the same values as loading the component directly", () => {
    const { result: converted } = renderHook(() =>
      useForm<ComponentRepresentation>(),
    );
    const { result: reset } = renderHook(() =>
      useForm<ComponentRepresentation>(),
    );

    act(() => customProviderLoad(converted.current));
    act(() => ldapLoad(reset.current));

    for (const key of [
      "cachePolicy",
      "evictionDay",
      "evictionHour",
      "evictionMinute",
      "maxLifespan",
    ]) {
      expect(converted.current.getValues(`config.${key}`)).toEqual(
        reset.current.getValues(`config.${key}`),
      );
    }
  });

  it("leaves the provider properties unwrapped", () => {
    const { result } = renderHook(() => useForm<ComponentRepresentation>());

    act(() => customProviderLoad(result.current));

    expect(result.current.getValues("config.someProviderProperty")).toBe(
      "a value",
    );
  });

  it("does not add entries the component does not have", () => {
    const { result } = renderHook(() => useForm<ComponentRepresentation>());

    act(() =>
      setupCacheForm(
        { config: { cachePolicy: ["NO_CACHE"] } },
        result.current.setValue,
      ),
    );

    expect(result.current.getValues("config")).toEqual({
      cachePolicy: ["NO_CACHE"],
    });
  });

  it("does not mark the form as dirty", () => {
    const { result } = renderHook(() => useForm<ComponentRepresentation>());

    act(() => customProviderLoad(result.current));

    expect(result.current.formState.isDirty).toBe(false);
  });
});

/**
 * Registering a control on an index path writes the resolved value back into
 * the form, so an unwrapped entry is truncated by the mere act of rendering the
 * field: `config.maxLifespan[0]` resolves against the string "50" as "5", and
 * that is what the next save sends. `shared/Header.tsx` saves from the
 * enable/disable toggle without a dirty check, so no edit is needed for this to
 * be persisted. These render both load paths through the real controls to cover
 * that, which the form value assertions above cannot see.
 */
describe("SettingsCache", () => {
  beforeAll(async () => {
    await i18next.use(initReactI18next).init({ lng: "en", resources: {} });
    // The `Help` provider the labels need reads Web Storage, which jsdom does
    // not provide here.
    const items = new Map<string, string>();
    vi.stubGlobal("localStorage", {
      getItem: (key: string) => items.get(key) ?? null,
      setItem: (key: string, value: string) => items.set(key, value),
      removeItem: (key: string) => items.delete(key),
    });
  });

  afterAll(() => vi.unstubAllGlobals());

  afterEach(cleanup);

  const renderCacheFields = (
    load: (form: UseFormReturn<ComponentRepresentation>) => void,
    onSave: (values: ComponentRepresentation) => void,
  ) => {
    const Harness = () => {
      const form = useForm<ComponentRepresentation>();
      // The custom provider screen renders a spinner until the fetched
      // component has been put into the form, so the fields never mount
      // against an unpopulated form.
      const [loaded, setLoaded] = useState(false);
      useEffect(() => {
        load(form);
        setLoaded(true);
      }, [form]);

      return loaded ? (
        <Help>
          <FormProvider {...form}>
            <form onSubmit={form.handleSubmit(onSave)}>
              <SettingsCache form={form} unWrap />
              <button type="submit">save</button>
            </form>
          </FormProvider>
        </Help>
      ) : null;
    };

    return render(<Harness />);
  };

  const savedLifespan = (onSave: ReturnType<typeof vi.fn>) => {
    const saved = onSave.mock.calls[0][0].config.maxLifespan;
    return Array.isArray(saved) ? saved[0] : saved;
  };

  it.each(LOAD_PATHS)("shows the stored lifespan (%s)", async (_, load) => {
    renderCacheFields(load, vi.fn());

    const input = await screen.findByRole<HTMLInputElement>("spinbutton");

    expect(input.value).toBe("50");
  });

  it.each(LOAD_PATHS)(
    "saves the stored lifespan when it is not edited (%s)",
    async (_, load) => {
      const onSave = vi.fn();
      renderCacheFields(load, onSave);
      await screen.findByRole("spinbutton");

      fireEvent.click(screen.getByRole("button", { name: "save" }));

      await waitFor(() => expect(onSave).toHaveBeenCalled());
      expect(savedLifespan(onSave)).toBe("50");
    },
  );

  it.each(LOAD_PATHS)("saves an edited lifespan (%s)", async (_, load) => {
    const onSave = vi.fn();
    renderCacheFields(load, onSave);
    const input = await screen.findByRole("spinbutton");

    fireEvent.change(input, { target: { value: "120" } });
    fireEvent.click(screen.getByRole("button", { name: "save" }));

    await waitFor(() => expect(onSave).toHaveBeenCalled());
    expect(savedLifespan(onSave)).toBe(120);
  });
});
