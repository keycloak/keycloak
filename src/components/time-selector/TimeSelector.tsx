import {
  Select,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
  TextInput,
  TextInputProps,
} from "@patternfly/react-core";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

export type Unit = "seconds" | "minutes" | "hours" | "days";

export type TimeSelectorProps = TextInputProps & {
  value: number;
  units?: Unit[];
  onChange: (time: number | string) => void;
};

export const TimeSelector = ({
  value,
  units = ["seconds", "minutes", "hours", "days"],
  onChange,
  ...rest
}: TimeSelectorProps) => {
  const { t } = useTranslation("common");

  const allTimes: { unit: Unit; label: string; multiplier: number }[] = [
    { unit: "seconds", label: t("times.seconds"), multiplier: 1 },
    { unit: "minutes", label: t("times.minutes"), multiplier: 60 },
    { unit: "hours", label: t("times.hours"), multiplier: 3600 },
    { unit: "days", label: t("times.days"), multiplier: 86400 },
  ];

  const times = units.map(
    (unit) => allTimes.find((time) => time.unit === unit)!
  );
  const defaultMultiplier = allTimes.find(
    (time) => time.unit === units[0]
  )?.multiplier;

  const [timeValue, setTimeValue] = useState<"" | number>("");
  const [multiplier, setMultiplier] = useState(defaultMultiplier);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const x = times.reduce(
      (v, time) =>
        value % time.multiplier === 0 && v < time.multiplier
          ? time.multiplier
          : v,
      1
    );

    if (value) {
      setMultiplier(x);
      setTimeValue(value / x);
    } else {
      setTimeValue("");
      setMultiplier(defaultMultiplier);
    }
  }, [value]);

  const updateTimeout = (
    timeout: "" | number,
    times: number | undefined = multiplier
  ) => {
    if (timeout !== "") {
      onChange(timeout * (times || 1));
      setTimeValue(timeout);
    } else {
      onChange("");
    }
  };

  return (
    <Split hasGutter>
      <SplitItem>
        <TextInput
          {...rest}
          type="number"
          id={`kc-time-${new Date().getTime()}`}
          min="0"
          value={timeValue}
          onChange={(value) => {
            updateTimeout("" === value ? value : parseInt(value));
          }}
        />
      </SplitItem>
      <SplitItem>
        <Select
          variant={SelectVariant.single}
          aria-label={t("unitLabel")}
          onSelect={(_, value) => {
            setMultiplier(value as number);
            updateTimeout(timeValue, value as number);
            setOpen(false);
          }}
          selections={[multiplier]}
          onToggle={() => {
            setOpen(!open);
          }}
          isOpen={open}
        >
          {times.map((time) => (
            <SelectOption key={time.label} value={time.multiplier}>
              {time.label}
            </SelectOption>
          ))}
        </Select>
      </SplitItem>
    </Split>
  );
};
