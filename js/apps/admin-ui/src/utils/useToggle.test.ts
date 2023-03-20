/**
 * @vitest-environment jsdom
 */
import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import useToggle from "./useToggle";

describe("useToggle", () => {
  it("has a default value of false", () => {
    const { result } = renderHook(() => useToggle());
    const [value] = result.current;

    expect(value).toBe(false);
  });

  it("uses the initial value", () => {
    const { result } = renderHook(() => useToggle(true));
    const [value] = result.current;

    expect(value).toBe(true);
  });

  it("toggles the value", () => {
    const { result } = renderHook(() => useToggle());
    const [, toggleValue] = result.current;

    act(() => toggleValue());

    const [value] = result.current;
    expect(value).toBe(true);
  });

  it("sets the value", () => {
    const { result } = renderHook(() => useToggle());
    const [, , setValue] = result.current;

    act(() => setValue(true));

    const [value] = result.current;
    expect(value).toBe(true);
  });
});
