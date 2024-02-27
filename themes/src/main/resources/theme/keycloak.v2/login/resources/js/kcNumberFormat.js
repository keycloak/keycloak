// @ts-check
import { formatNumber } from "./common.js";
import Alpine from "../script/module.esm.js";

const KC_NUMBER_FORMAT = "kcNumberFormat"

function formatElement(event) {
  const format = event.target.getAttribute(`data-${KC_NUMBER_FORMAT}`);
  if (format) {
    return formatNumber(event.target.value, format);
  }

  return event.target.value;
}

document.addEventListener('alpine:init', () => {
  Alpine.store('format', { formatElement })
})

Alpine.start()
