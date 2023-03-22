import { describe, expect, it } from "vitest";
import { getTimeUnit, toHumanFormat } from "./TimeSelector";

describe("Time conversion functions", () => {
  it("should convert milliseconds to unit", () => {
    const givenTime = 86400;

    //when
    const timeUnit = getTimeUnit(givenTime);

    //then
    expect(timeUnit.unit).toEqual("day");
  });

  it("should convert to human format", () => {
    const givenTime = 86400 * 2;

    //when
    const timeString = toHumanFormat(givenTime, "en");

    //then
    expect(timeString).toEqual("2 days");
  });
});
