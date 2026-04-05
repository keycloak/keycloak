// @ts-check
import { formatNumber } from "./common.js";
import { registerElementAnnotatedBy } from "./userProfile.js";

const KC_NUMBER_FORMAT = "kcNumberFormat";

registerElementAnnotatedBy({
  name: KC_NUMBER_FORMAT,
  onAdd(element) {
    const formatValue = () => {
      const format = element.getAttribute(`data-${KC_NUMBER_FORMAT}`);
      element.value = formatNumber(element.value, format);
    };

    element.addEventListener("keyup", formatValue);

    formatValue();

    return () => element.removeEventListener("keyup", formatValue);
  },
});
