import Alpine from "./module.esm.js";

import { formatNumber } from "../js/common.js";
import { unFormat } from "../js/kcNumberUnFormat.js";

const KC_NUMBER_FORMAT = "kcNumberFormat"

function formatElement(event) {
  const format = event.target.getAttribute(`data-${KC_NUMBER_FORMAT}`);
  return formatNumber(event.target.value, format);
}

document.addEventListener("bind", unFormat);

document.addEventListener('alpine:init', () => {
  Alpine.store('format', { formatElement })
})

Alpine.start()