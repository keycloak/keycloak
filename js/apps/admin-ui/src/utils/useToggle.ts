import { useCallback, useState } from "react";

/**
 * A hook that allows you toggle a boolean value, useful for toggle buttons, showing and hiding modals, etc.
 *
 * @param initialValue The initial value to use, false by default.
 */
export default function useToggle(initialValue = false) {
  const [value, setValue] = useState(initialValue);
  const toggleValue = useCallback(() => setValue((val) => !val), []);

  return [value, toggleValue, setValue] as const;
}
