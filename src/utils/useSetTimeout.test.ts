/**
 * @jest-environment jsdom
 */
import { renderHook } from "@testing-library/react-hooks";
import useSetTimeout from "./useSetTimeout";

jest.useFakeTimers();

describe("useSetTimeout", () => {
  it("schedules timeouts and triggers the callbacks", () => {
    const { result } = renderHook(() => useSetTimeout());
    const setTimeoutSpy = jest.spyOn(global, "setTimeout");

    // Schedule some timeouts...
    const callback1 = jest.fn();
    const callback2 = jest.fn();
    result.current(callback1, 1000);
    result.current(callback2, 500);

    // Ensure that setTimeout was actually called with the correct arguments.
    expect(setTimeoutSpy).toHaveBeenCalledTimes(2);
    expect(setTimeoutSpy).toBeCalledWith(expect.any(Function), 1000);
    expect(setTimeoutSpy).toBeCalledWith(expect.any(Function), 500);

    // Ensure callbacks are called after timers run.
    expect(callback2).not.toBeCalled();
    jest.advanceTimersByTime(500);
    expect(callback1).not.toBeCalled();
    expect(callback2).toBeCalled();
    jest.advanceTimersByTime(500);
    expect(callback1).toBeCalled();

    setTimeoutSpy.mockRestore();
  });

  it("throws if a timeout is scheduled after the component has unmounted", () => {
    const { result, unmount } = renderHook(() => useSetTimeout());

    unmount();

    expect(() => result.current(jest.fn(), 1000)).toThrowError(
      "Can't schedule a timeout on an unmounted component."
    );
  });

  it("clears a timeout if the component unmounts", () => {
    const { result, unmount } = renderHook(() => useSetTimeout());
    const timerId = 42;
    const setTimeoutSpy = jest
      .spyOn(global, "setTimeout")
      .mockReturnValueOnce(timerId as unknown as NodeJS.Timeout);
    const clearTimeoutSpy = jest.spyOn(global, "clearTimeout");

    result.current(jest.fn(), 1000);
    unmount();

    expect(clearTimeoutSpy).toBeCalledWith(timerId);

    setTimeoutSpy.mockRestore();
  });
});
