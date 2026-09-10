import { describe, expect, it } from "vitest";
import { unversionedName } from "./useIsFeatureEnabled";

describe("unversionedName", () => {
  it("strips version suffix from feature names", () => {
    expect(unversionedName("ACCOUNT_V3")).toBe("ACCOUNT");
    expect(unversionedName("ADMIN_FINE_GRAINED_AUTHZ_V2")).toBe(
      "ADMIN_FINE_GRAINED_AUTHZ",
    );
    expect(unversionedName("TOKEN_EXCHANGE_STANDARD_V2")).toBe(
      "TOKEN_EXCHANGE_STANDARD",
    );
  });

  it("returns name unchanged when there is no version suffix", () => {
    expect(unversionedName("ACCOUNT_API")).toBe("ACCOUNT_API");
    expect(unversionedName("ORGANIZATION")).toBe("ORGANIZATION");
    expect(unversionedName("DPOP")).toBe("DPOP");
    expect(unversionedName("CLIENT_POLICIES")).toBe("CLIENT_POLICIES");
  });

  it("only strips trailing version suffix", () => {
    expect(unversionedName("V2_SOMETHING")).toBe("V2_SOMETHING");
    expect(unversionedName("FEATURE_V2_EXTRA")).toBe("FEATURE_V2_EXTRA");
  });
});
