// eslint-disable-next-line no-restricted-imports, @typescript-eslint/no-unused-vars
import * as React from "react";
import { render } from "@testing-library/react";
import { FlowDiagram } from "../components/FlowDiagram";
import { describe, expect, it, beforeEach } from "vitest";
import { ExecutionList } from "../execution-model";

// mock react-flow
// code from https://reactflow.dev/learn/advanced-use/testing
class ResizeObserver {
  callback: globalThis.ResizeObserverCallback;

  constructor(callback: globalThis.ResizeObserverCallback) {
    this.callback = callback;
  }

  observe(target: Element) {
    this.callback([{ target } as globalThis.ResizeObserverEntry], this);
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  unobserve() {}
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  disconnect() {}
}

class DOMMatrixReadOnly {
  m22: number;
  constructor(transform: string) {
    const scale = transform.match(/scale\(([1-9.])\)/)?.[1];
    this.m22 = scale !== undefined ? +scale : 1;
  }
}

// Only run the shim once when requested
let init = false;

export const mockReactFlow = () => {
  if (init) return;
  init = true;

  global.ResizeObserver = ResizeObserver;

  // @ts-ignore
  global.DOMMatrixReadOnly = DOMMatrixReadOnly;

  Object.defineProperties(global.HTMLElement.prototype, {
    offsetHeight: {
      get() {
        return parseFloat(this.style.height) || 1;
      },
    },
    offsetWidth: {
      get() {
        return parseFloat(this.style.width) || 1;
      },
    },
  });

  (global.SVGElement as any).prototype.getBBox = () => ({
    x: 0,
    y: 0,
    width: 0,
    height: 0,
  });
};

describe("<FlowDiagram />", () => {
  beforeEach(() => {
    mockReactFlow();
  });

  const reactFlowTester = (container: HTMLElement) => ({
    expectEdgeLabels: (expectedEdges: string[]) => {
      const edges = Array.from(
        container.getElementsByClassName("react-flow__edge"),
      );
      expect(
        edges.map((edge) => edge.getAttribute("aria-label")).sort(),
      ).toEqual(expectedEdges.sort());
    },
    expectNodeIds: (expectedNodes: string[]) => {
      const nodes = Array.from(
        container.getElementsByClassName("react-flow__node"),
      );
      expect(nodes.map((node) => node.getAttribute("data-id")).sort()).toEqual(
        expectedNodes.sort(),
      );
    },
  });

  it("should render a flow with one required step", () => {
    const executionList = new ExecutionList([
      { id: "single", displayName: "Single", level: 0 },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    // const nodes = Array.from(container.getElementsByClassName("react-flow__node"));
    const testHelper = reactFlowTester(container);

    const expectedEdges = [
      "Edge from start to single",
      "Edge from single to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);

    const expectedNodes = new Set(["start", "single", "end"]);
    testHelper.expectNodeIds(Array.from(expectedNodes));
  });

  it("should render a start connected to end with no steps", () => {
    const executionList = new ExecutionList([]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);

    const expectedEdges = ["Edge from start to end"];
    testHelper.expectEdgeLabels(expectedEdges);

    const expectedNodes = new Set(["start", "end"]);
    testHelper.expectNodeIds(Array.from(expectedNodes));
  });

  it("should render two branches with two alternative steps", () => {
    const executionList = new ExecutionList([
      {
        id: "alt1",
        displayName: "Alt1",
        requirement: "ALTERNATIVE",
      },
      {
        id: "alt2",
        displayName: "Alt2",
        requirement: "ALTERNATIVE",
      },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);

    const expectedEdges = [
      "Edge from start to alt1",
      "Edge from alt1 to end",
      "Edge from start to alt2",
      "Edge from alt2 to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);

    const expectedNodes = new Set(["start", "alt1", "alt2", "end"]);
    testHelper.expectNodeIds(Array.from(expectedNodes));
  });

  it("should render a flow with a subflow", () => {
    const executionList = new ExecutionList([
      {
        id: "requiredElement",
        displayName: "Required Element",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "subflow",
        displayName: "Subflow",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "subElement",
        displayName: "Sub Element",
        requirement: "REQUIRED",
        level: 1,
      },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);
    const expectedNodes = ["start", "requiredElement", "subElement", "end"];
    testHelper.expectNodeIds(expectedNodes);

    const expectedEdges = [
      "Edge from start to requiredElement",
      "Edge from requiredElement to subElement",
      "Edge from subElement to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);
  });

  it("should render a flow with a subflow with alternative steps", () => {
    const executionList = new ExecutionList([
      {
        id: "requiredElement",
        displayName: "Required Element",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "subflow",
        displayName: "Subflow",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "subElement1",
        displayName: "Sub Element",
        requirement: "ALTERNATIVE",
        level: 1,
      },
      {
        id: "subElement2",
        displayName: "Sub Element",
        requirement: "ALTERNATIVE",
        level: 1,
      },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);
    const expectedEdges = [
      "Edge from start to requiredElement",
      "Edge from requiredElement to subElement1",
      "Edge from subElement1 to end",
      "Edge from requiredElement to subElement2",
      "Edge from subElement2 to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);

    const expectedNodes = [
      "start",
      "requiredElement",
      "subElement1",
      "subElement2",
      "end",
    ];
    testHelper.expectNodeIds(expectedNodes);
  });

  it("should render a flow with a subflow with alternative steps and combine to a required step", () => {
    const executionList = new ExecutionList([
      {
        id: "requiredElement",
        displayName: "Required Element",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "subflow",
        displayName: "Subflow",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "subElement1",
        displayName: "Sub Element",
        requirement: "ALTERNATIVE",
        level: 1,
      },
      {
        id: "subElement2",
        displayName: "Sub Element",
        requirement: "ALTERNATIVE",
        level: 1,
      },
      {
        id: "finalStep",
        displayName: "Final Step",
        requirement: "REQUIRED",
        level: 0,
      },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);
    const expectedEdges = [
      "Edge from start to requiredElement",
      "Edge from requiredElement to subElement1",
      "Edge from subElement1 to finalStep",
      "Edge from requiredElement to subElement2",
      "Edge from subElement2 to finalStep",
      "Edge from finalStep to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);

    const expectedNodes = [
      "start",
      "requiredElement",
      "subElement1",
      "subElement2",
      "finalStep",
      "end",
    ];
    testHelper.expectNodeIds(expectedNodes);
  });

  it("should render a flow with a conditional subflow followed by a required step", () => {
    const executionList = new ExecutionList([
      {
        id: "chooseUser",
        displayName: "Required Element",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "sendReset",
        displayName: "Send Reset",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "conditionalOTP",
        displayName: "Conditional OTP",
        requirement: "CONDITIONAL",
        level: 0,
      },
      {
        id: "conditionOtpConfigured",
        displayName: "Condition - User Configured",
        requirement: "REQUIRED",
        level: 1,
      },
      {
        id: "otpForm",
        displayName: "OTP Form",
        requirement: "REQUIRED",
        level: 1,
      },
      {
        id: "resetPassword",
        displayName: "Reset Password",
        requirement: "REQUIRED",
        level: 0,
      },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);
    const expectedNodes = [
      "start",
      "chooseUser",
      "sendReset",
      "conditionOtpConfigured",
      "otpForm",
      "resetPassword",
      "end",
    ];
    testHelper.expectNodeIds(expectedNodes);

    const expectedEdges = [
      "Edge from start to chooseUser",
      "Edge from chooseUser to sendReset",
      "Edge from sendReset to conditionOtpConfigured",
      "Edge from conditionOtpConfigured to otpForm",
      "Edge from conditionOtpConfigured to resetPassword",
      "Edge from otpForm to resetPassword",
      "Edge from resetPassword to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);
  });

  it("should render a complex flow with serial conditionals", () => {
    // flow inspired by ![conditional flow PR](https://github.com/keycloak/keycloak/pull/28481)
    const executionList = new ExecutionList([
      {
        id: "exampleForms",
        displayName: "Example Forms",
        requirement: "ALTERNATIVE",
        level: 0,
      },
      {
        id: "usernamePasswordForm",
        displayName: "Username Password Form",
        requirement: "REQUIRED",
        level: 1,
      },
      {
        id: "conditionalOTP",
        displayName: "Conditional OTP",
        requirement: "CONDITIONAL",
        level: 1,
      },
      {
        id: "conditionUserConfigured",
        displayName: "Condition - User Configured",
        requirement: "REQUIRED",
        level: 2,
      },
      {
        id: "conditionUserAttribute",
        displayName: "Condition - User Attribute",
        requirement: "REQUIRED",
        level: 2,
      },
      {
        id: "otpForm",
        displayName: "OTP Form",
        requirement: "REQUIRED",
        level: 2,
      },
      {
        id: "confirmLink",
        displayName: "Confirm Link",
        requirement: "REQUIRED",
        level: 2,
      },
      {
        id: "conditionalReviewProfile",
        displayName: "Conditional Review Profile",
        requirement: "CONDITIONAL",
        level: 0,
      },
      {
        id: "conditionLoa",
        displayName: "Condition - Loa",
        requirement: "REQUIRED",
        level: 1,
      },
      {
        id: "reviewProfile",
        displayName: "Review Profile",
        requirement: "REQUIRED",
        level: 1,
      },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);

    const expectedNodes = [
      "start",
      "usernamePasswordForm",
      "conditionUserConfigured",
      "conditionUserAttribute",
      "otpForm",
      "confirmLink",
      "conditionLoa",
      "reviewProfile",
      "end",
    ];
    testHelper.expectNodeIds(expectedNodes);

    const expectedEdges = [
      "Edge from start to usernamePasswordForm",
      "Edge from usernamePasswordForm to conditionUserConfigured",
      "Edge from conditionUserConfigured to conditionUserAttribute",
      "Edge from conditionUserConfigured to end",
      "Edge from conditionUserAttribute to otpForm",
      "Edge from conditionUserAttribute to end",
      "Edge from otpForm to confirmLink",
      "Edge from confirmLink to end",
      "Edge from start to conditionLoa",
      "Edge from conditionLoa to reviewProfile",
      "Edge from conditionLoa to end",
      "Edge from reviewProfile to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);
  });

  it("should render the default first broker login flow", () => {
    const executionList = new ExecutionList([
      {
        id: "reviewProfile",
        displayName: "Review Profile",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "createOrLink",
        displayName: "User creation or linking",
        requirement: "REQUIRED",
        level: 0,
      },
      {
        id: "createUnique",
        displayName: "Create User If Unique",
        requirement: "ALTERNATIVE",
        level: 1,
      },
      {
        id: "existingAccount",
        displayName: "Handle Existing Account",
        requirement: "ALTERNATIVE",
        level: 1,
      },
      {
        id: "confirmLink",
        displayName: "Confirm link existing account",
        requirement: "REQUIRED",
        level: 2,
      },
      {
        id: "accountVerification",
        displayName: "Account verification options",
        requirement: "REQUIRED",
        level: 2,
      },
      {
        id: "emailVerify",
        displayName: "Verify existing account by Email",
        requirement: "ALTERNATIVE",
        level: 3,
      },
      {
        id: "reauthVerify",
        displayName: "Verify Existing Account by Re-authentication",
        requirement: "ALTERNATIVE",
        level: 3,
      },
      {
        id: "usernamePassword",
        displayName:
          "Username Password Form for identity provider reauthentication",
        requirement: "REQUIRED",
        level: 4,
      },
      {
        id: "conditionalOtp",
        displayName: "First broker login - Conditional OTP",
        requirement: "CONDITIONAL",
        level: 4,
      },
      {
        id: "conditionUserConfigured",
        displayName: "Condition - user configured",
        requirement: "REQUIRED",
        level: 5,
      },
      {
        id: "otpForm",
        displayName: "OTP Form",
        requirement: "REQUIRED",
        level: 5,
      },
    ]);

    const { container } = render(<FlowDiagram executionList={executionList} />);

    const testHelper = reactFlowTester(container);

    const expectedNodes = [
      "start",
      "reviewProfile",
      "createUnique",
      "confirmLink",
      "usernamePassword",
      "conditionUserConfigured",
      "otpForm",
      "emailVerify",
      "end",
    ];
    testHelper.expectNodeIds(expectedNodes);

    const expectedEdges = [
      "Edge from start to reviewProfile",
      "Edge from reviewProfile to createUnique",
      "Edge from reviewProfile to confirmLink",
      "Edge from createUnique to end",
      "Edge from confirmLink to emailVerify",
      "Edge from confirmLink to usernamePassword",
      "Edge from usernamePassword to conditionUserConfigured",
      "Edge from conditionUserConfigured to otpForm",
      "Edge from conditionUserConfigured to end",
      "Edge from otpForm to end",
      "Edge from emailVerify to end",
    ];
    testHelper.expectEdgeLabels(expectedEdges);
  });
});
