import { render, waitFor } from "@testing-library/react";
import React from "react";
import { MockAdminClient } from "../../stories/MockAdminClient";
import { DataLoader } from "./DataLoader";

describe("DataLoader", () => {
  it("loads the data and renders the result", async () => {
    const loader = () => Promise.resolve(["a", "b"]);
    const { container } = render(
      <MockAdminClient>
        <DataLoader loader={loader}>
          {(result) => (
            <div>
              {result.map((value) => (
                <i key={value}>{value}</i>
              ))}
            </div>
          )}
        </DataLoader>
      </MockAdminClient>
    );

    await waitFor(() => expect(container.textContent).toEqual("ab"));
  });
});
