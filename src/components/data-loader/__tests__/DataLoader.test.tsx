import React from "react";
import { DataLoader } from "../DataLoader";
import { act } from "@testing-library/react";
import { render, unmountComponentAtNode } from "react-dom";

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
        <DataLoader loader={loader}>
          {(result) => (
            <div>
              {result.data.map((d, i) => (
                <i key={i}>{d}</i>
              ))}
            </div>
          )}
        </DataLoader>,
        container
      );
    });
    expect(container.textContent).toBe("ab");
  });
});
