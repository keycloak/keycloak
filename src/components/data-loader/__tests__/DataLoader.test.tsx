import React from "react";
import { DataLoader } from "../DataLoader";
import { act } from "@testing-library/react";
import { render, unmountComponentAtNode } from "react-dom";
import { MockAdminClient } from "../../../stories/MockAdminClient";

let container: HTMLDivElement;
beforeEach(() => {
  container = document.createElement("div");
  document.body.appendChild(container);
});

afterEach(() => {
  unmountComponentAtNode(container);
  container.remove();
});

describe("<DataLoader />", () => {
  it("render", async () => {
    const loader = () => Promise.resolve(["a", "b"]);
    await act(async () => {
      render(
        <MockAdminClient>
          <DataLoader loader={loader}>
            {(result) => (
              <div>
                {result.map((d, i) => (
                  <i key={i}>{d}</i>
                ))}
              </div>
            )}
          </DataLoader>
        </MockAdminClient>,
        container
      );
    });
    expect(container.textContent).toBe("ab");
  });
});
