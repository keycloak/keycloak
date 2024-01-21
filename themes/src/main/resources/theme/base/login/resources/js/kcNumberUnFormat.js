import {formatNumber} from "./common.js";

const DATA_KC_NUMBER_UNFORMAT = 'data-kcNumberUnFormat';

document.querySelectorAll(`[${DATA_KC_NUMBER_UNFORMAT}]`)
    .forEach(input => {
        for (let form of document.forms) {
            form.addEventListener('submit', (event) => {
                const rawFormat = input.getAttribute(DATA_KC_NUMBER_UNFORMAT);
                if (rawFormat) {
                    input.value = formatNumber(input.value, rawFormat);
                }
            });
        }
    });