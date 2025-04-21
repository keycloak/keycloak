export const amSuffix = ' AM';
export const pmSuffix = ' PM';
export const makeTimeOptions = (stepMinutes, hour12, delimiter, minTime, maxTime, includeSeconds) => {
    const res = [];
    const iter = new Date(new Date().setHours(0, 0, 0, 0));
    const iterDay = iter.getDay();
    while (iter.getDay() === iterDay) {
        let hour = iter.getHours();
        let suffix = amSuffix;
        if (hour12) {
            if (hour === 0) {
                hour = 12; // 12am
            }
            else if (hour >= 12) {
                suffix = pmSuffix;
            }
            if (hour > 12) {
                hour %= 12;
            }
        }
        hour = hour12 ? hour.toString() : hour.toString().padStart(2, '0');
        const minutes = iter
            .getMinutes()
            .toString()
            .padStart(2, '0');
        const timeOption = `${hour}${delimiter}${minutes}${hour12 ? suffix : ''}`;
        // time option is valid if within min/max constraints
        if (isWithinMinMax(minTime, maxTime, timeOption, delimiter, includeSeconds)) {
            res.push(timeOption);
        }
        iter.setMinutes(iter.getMinutes() + stepMinutes);
    }
    return res;
};
export const parseTime = (time, timeRegex, delimiter, is12Hour, includeSeconds) => {
    const date = new Date(time);
    // if default time is a ISO 8601 formatted date string, we parse it to hh:mm(am/pm) format
    if (!isNaN(date.getDate()) && (time instanceof Date || time.includes('T'))) {
        const hours = is12Hour
            ? `${date.getHours() > 12 ? date.getHours() - 12 : date.getHours()}`
            : `${date.getHours()}`.padStart(2, '0');
        const minutes = `${date.getMinutes()}`.padStart(2, '0');
        const seconds = includeSeconds ? `${date.getSeconds()}`.padStart(2, '0') : '';
        const secondsWithDelimiter = seconds ? `${delimiter}${seconds}` : '';
        let ampm = '';
        if (is12Hour && date.getHours() > 11) {
            ampm = pmSuffix;
        }
        else if (is12Hour) {
            ampm = amSuffix;
        }
        return `${hours}${delimiter}${minutes}${secondsWithDelimiter}${ampm}`;
    }
    else if (typeof time === 'string') {
        time = time.trim();
        if (time !== '' && validateTime(time, timeRegex, delimiter, is12Hour)) {
            const [, hours, minutes, seconds, suffix = ''] = timeRegex.exec(time);
            const secondsWithDelimiter = includeSeconds ? `${delimiter}${seconds !== null && seconds !== void 0 ? seconds : '00'}` : '';
            let ampm = '';
            // Format AM/PM according to design
            if (is12Hour) {
                const uppercaseSuffix = suffix.toUpperCase();
                if (uppercaseSuffix === amSuffix.toUpperCase().trim()) {
                    ampm = amSuffix;
                }
                else if (uppercaseSuffix === pmSuffix.toUpperCase().trim()) {
                    ampm = pmSuffix;
                }
                else {
                    // if this 12 hour time is missing am/pm but otherwise valid,
                    // append am/pm depending on time of day
                    ampm = new Date().getHours() > 11 ? pmSuffix : amSuffix;
                }
            }
            return `${hours}${delimiter}${minutes}${secondsWithDelimiter}${ampm}`;
        }
    }
    return time.toString();
};
export const validateTime = (time, timeRegex, delimiter, is12Hour) => {
    // ISO 8601 format is valid
    const date = new Date(time);
    if (!isNaN(date.getDate()) && time.includes('T')) {
        return true;
    }
    // hours only valid if they are [0-23] or [1-12]
    const hours = parseInt(time.split(delimiter)[0]);
    const validHours = hours >= (is12Hour ? 1 : 0) && hours <= (is12Hour ? 12 : 23);
    // minutes verified by timeRegex
    // empty string is valid
    return time === '' || (timeRegex.test(time) && validHours);
};
export const getHours = (time, timeRegex) => {
    const parts = time.match(timeRegex);
    if (parts && parts.length) {
        if (/pm/i.test(parts[4])) {
            return parseInt(parts[1]) === 12 ? parseInt(parts[1]) : parseInt(parts[1]) + 12;
        }
        if (/am/i.test(parts[4])) {
            return parseInt(parts[1]) === 12 ? 0 : parseInt(parts[1]);
        }
        return parseInt(parts[1]);
    }
    return null;
};
export const getMinutes = (time, timeRegex) => {
    const parts = time.match(timeRegex);
    return parts && parts.length ? parseInt(parts[2]) : null;
};
export const getSeconds = (time, timeRegex) => {
    var _a;
    const seconds = (_a = time.match(timeRegex)) === null || _a === void 0 ? void 0 : _a[3];
    return seconds ? parseInt(seconds) : null;
};
export const isWithinMinMax = (minTime, maxTime, time, delimiter, includeSeconds) => {
    // do not throw error if empty string
    if (time.trim() === '') {
        return true;
    }
    // correctly format as 24hr times (12:30AM => 00:30, 1:15 => 01:15)
    const min24HourTime = convertTo24Hour(minTime, delimiter, includeSeconds);
    const selected24HourTime = convertTo24Hour(time, delimiter, includeSeconds);
    const max24HourTime = convertTo24Hour(maxTime, delimiter, includeSeconds);
    // simple string comparison for 24hr times
    return min24HourTime <= selected24HourTime && selected24HourTime <= max24HourTime;
};
const convertTo24Hour = (time, delimiter, includeSeconds) => {
    const timeReg = new RegExp(`^\\s*(\\d\\d?)${delimiter}([0-5]\\d)${delimiter}?([0-5]\\d)?\\s*([AaPp][Mm])?\\s*$`);
    const regMatches = timeReg.exec(time);
    if (!regMatches || !regMatches.length) {
        return;
    }
    let hours = regMatches[1].padStart(2, '0');
    const minutes = regMatches[2];
    let seconds = regMatches[3] ? `${delimiter}${regMatches[3]}` : '';
    // When seconds is empty and 'includeSeconds' is enabled, append 0 seconds.
    if (!seconds && includeSeconds) {
        seconds = `${delimiter}00`;
    }
    const suffix = regMatches[4] || '';
    if (suffix.toUpperCase() === 'PM' && hours !== '12') {
        hours = `${parseInt(hours) + 12}`;
    }
    else if (suffix.toUpperCase() === 'AM' && hours === '12') {
        hours = '00';
    }
    return `${hours}${delimiter}${minutes}${seconds}`;
};
//# sourceMappingURL=TimePickerUtils.js.map