// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import { fail } from "assert";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { RequiredActionAlias } from "../src/defs/requiredActionProviderRepresentation.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Authentication management", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentRealm: string;
  let requiredActionProvider: Record<string, any>;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);
    const realmName = faker.internet.username().toLowerCase();
    await kcAdminClient.realms.create({
      id: realmName,
      realm: realmName,
      enabled: true,
    });
    currentRealm = realmName;
    kcAdminClient.setConfig({
      realmName,
    });
  });

  after(async () => {
    // delete test realm
    await kcAdminClient.realms.del({ realm: currentRealm });
    const realm = await kcAdminClient.realms.findOne({
      realm: currentRealm,
    });
    expect(realm).to.be.null;
  });

  /**
   * Required Actions
   */
  describe("Required Actions", () => {
    it("should delete required action by alias", async () => {
      await kcAdminClient.authenticationManagement.deleteRequiredAction({
        alias: RequiredActionAlias.UPDATE_PROFILE,
      });
    });

    it("should get unregistered required actions", async () => {
      const unregisteredReqActions =
        await kcAdminClient.authenticationManagement.getUnregisteredRequiredActions();
      expect(unregisteredReqActions).to.be.an("array");
      expect(unregisteredReqActions.length).to.be.least(1);
      requiredActionProvider = unregisteredReqActions[0];
    });

    it("should register new required action", async () => {
      const requiredAction =
        await kcAdminClient.authenticationManagement.registerRequiredAction({
          providerId: requiredActionProvider.providerId,
          name: requiredActionProvider.name,
        });
      expect(requiredAction).to.be.empty;
    });

    it("should get required actions", async () => {
      const requiredActions =
        await kcAdminClient.authenticationManagement.getRequiredActions();
      expect(requiredActions).to.be.an("array");
    });

    it("should get required action by alias", async () => {
      const requiredAction =
        await kcAdminClient.authenticationManagement.getRequiredActionForAlias({
          alias: requiredActionProvider.providerId,
        });
      expect(requiredAction).to.be.ok;
    });

    it("should update required action by alias", async () => {
      const requiredAction =
        await kcAdminClient.authenticationManagement.getRequiredActionForAlias({
          alias: requiredActionProvider.providerId,
        });
      const response =
        await kcAdminClient.authenticationManagement.updateRequiredAction(
          { alias: requiredActionProvider.providerId },
          {
            ...requiredAction,
            enabled: true,
            priority: 10,
          },
        );
      expect(response).to.be.empty;
    });

    it("should lower required action priority", async () => {
      const requiredAction =
        await kcAdminClient.authenticationManagement.getRequiredActionForAlias({
          alias: requiredActionProvider.providerId,
        });
      const response =
        await kcAdminClient.authenticationManagement.lowerRequiredActionPriority(
          { alias: requiredActionProvider.providerId },
        );
      expect(response).to.be.empty;
      const requiredActionUpdated =
        await kcAdminClient.authenticationManagement.getRequiredActionForAlias({
          alias: requiredActionProvider.providerId,
        });
      expect(requiredActionUpdated.priority).to.be.greaterThan(
        requiredAction.priority,
      );
    });

    it("should raise required action priority", async () => {
      const requiredAction =
        await kcAdminClient.authenticationManagement.getRequiredActionForAlias({
          alias: requiredActionProvider.providerId,
        });
      const response =
        await kcAdminClient.authenticationManagement.raiseRequiredActionPriority(
          { alias: requiredActionProvider.providerId },
        );
      expect(response).to.be.empty;
      const requiredActionUpdated =
        await kcAdminClient.authenticationManagement.getRequiredActionForAlias({
          alias: requiredActionProvider.providerId,
        });
      expect(requiredActionUpdated.priority).to.be.lessThan(
        requiredAction.priority,
      );
    });

    it("should fetch config description for required action", async () => {
      const configDescription =
        await kcAdminClient.authenticationManagement.getRequiredActionConfigDescription(
          {
            alias: "UPDATE_PASSWORD",
          },
        );

      expect(configDescription).is.ok;
      expect(configDescription.properties).is.ok;
    });

    it("should fetch required action config for update password", async () => {
      const actionConfig =
        await kcAdminClient.authenticationManagement.getRequiredActionConfig({
          alias: "UPDATE_PASSWORD",
        });

      expect(actionConfig).is.ok;
      expect(actionConfig.config).is.ok;
      expect(actionConfig.config!["max_auth_age"]).to.be.eq(300); // default max_auth_age for update password
    });

    it("should update required action config for update password", async () => {
      await kcAdminClient.authenticationManagement.updateRequiredActionConfig(
        {
          alias: "UPDATE_PASSWORD",
        },
        {
          config: {
            max_auth_age: "301",
          },
        },
      );

      const actionConfig =
        await kcAdminClient.authenticationManagement.getRequiredActionConfig({
          alias: "UPDATE_PASSWORD",
        });

      expect(actionConfig).is.ok;
      expect(actionConfig.config).is.ok;
      expect(actionConfig.config!["max_auth_age"]).to.be.eq(301); // updated value max_auth_age for update password
    });

    it("should reset required action config for update password", async () => {
      await kcAdminClient.authenticationManagement.removeRequiredActionConfig({
        alias: "UPDATE_PASSWORD",
      });

      const actionConfig =
        await kcAdminClient.authenticationManagement.getRequiredActionConfig({
          alias: "UPDATE_PASSWORD",
        });

      expect(actionConfig).is.ok;
      expect(actionConfig.config).is.ok;
      expect(actionConfig.config!["max_auth_age"]).to.be.eq(300); // default max_auth_age for update password
    });

    it("should get client authenticator providers", async () => {
      const authenticationProviders =
        await kcAdminClient.authenticationManagement.getClientAuthenticatorProviders();

      expect(authenticationProviders).is.ok;
      expect(authenticationProviders.length).to.be.equal(4);
    });

    it("should fetch form providers", async () => {
      const formProviders =
        await kcAdminClient.authenticationManagement.getFormActionProviders();
      expect(formProviders).is.ok;
      expect(formProviders.length).to.be.eq(4);
    });

    it("should fetch authenticator providers", async () => {
      const providers =
        await kcAdminClient.authenticationManagement.getAuthenticatorProviders();
      expect(providers).is.ok;
      expect(providers.length).to.be.greaterThan(1);
    });
  });
  describe("Flows", () => {
    it("should get the registered form providers", async () => {
      const formProviders =
        await kcAdminClient.authenticationManagement.getFormProviders();

      expect(formProviders).to.be.ok;
      expect(formProviders.length).to.be.eq(1);
      expect(formProviders[0].displayName).to.be.eq("Registration Page");
    });

    it("should get authentication flows", async () => {
      const flows = await kcAdminClient.authenticationManagement.getFlows();

      expect(flows.map((flow) => flow.alias)).to.be.deep.eq([
        "browser",
        "direct grant",
        "registration",
        "reset credentials",
        "clients",
        "first broker login",
        "docker auth",
      ]);
    });

    it("should get authentication flow", async () => {
      const flows = await kcAdminClient.authenticationManagement.getFlows();
      const flow = await kcAdminClient.authenticationManagement.getFlow({
        flowId: flows[0].id!,
      });

      expect(flow.alias).to.be.eq("browser");
    });

    it("should create new authentication flow", async () => {
      const flow = "test";
      await kcAdminClient.authenticationManagement.createFlow({
        alias: flow,
        providerId: "basic-flow",
        description: "",
        topLevel: true,
        builtIn: false,
      });

      const flows = await kcAdminClient.authenticationManagement.getFlows();
      expect(flows.find((f) => f.alias === flow)).to.be.ok;
    });

    const flowName = "copy of browser";
    it("should copy existing authentication flow", async () => {
      await kcAdminClient.authenticationManagement.copyFlow({
        flow: "browser",
        newName: flowName,
      });

      const flows = await kcAdminClient.authenticationManagement.getFlows();
      const flow = flows.find((f) => f.alias === flowName);
      expect(flow).to.be.ok;
    });

    it("should update authentication flow", async () => {
      const flows = await kcAdminClient.authenticationManagement.getFlows();
      const flow = flows.find((f) => f.alias === flowName)!;
      const description = "Updated description";
      flow.description = description;
      const updatedFlow =
        await kcAdminClient.authenticationManagement.updateFlow(
          { flowId: flow.id! },
          flow,
        );

      expect(updatedFlow.description).to.be.eq(description);
    });

    it("should delete authentication flow", async () => {
      let flows = await kcAdminClient.authenticationManagement.getFlows();
      const flow = flows.find((f) => f.alias === flowName)!;
      await kcAdminClient.authenticationManagement.deleteFlow({
        flowId: flow.id!,
      });

      flows = await kcAdminClient.authenticationManagement.getFlows();
      expect(flows.find((f) => f.alias === flowName)).to.be.undefined;
    });
  });
  describe("Flow executions", () => {
    it("should fetch all executions for a flow", async () => {
      const executions =
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: "browser",
        });
      expect(executions.length).to.be.gt(5);
    });

    const flowName = "executionTest";
    it("should add execution to a flow", async () => {
      await kcAdminClient.authenticationManagement.copyFlow({
        flow: "browser",
        newName: flowName,
      });
      const execution =
        await kcAdminClient.authenticationManagement.addExecutionToFlow({
          flow: flowName,
          provider: "auth-otp-form",
        });

      expect(execution.id).to.be.ok;
    });

    it("should add flow to a flow", async () => {
      const flow = await kcAdminClient.authenticationManagement.addFlowToFlow({
        flow: flowName,
        alias: "subFlow",
        description: "",
        provider: "registration-page-form",
        type: "basic-flow",
      });
      const executions =
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: flowName,
        });
      expect(flow.id).to.be.ok;

      expect(executions.map((execution) => execution.displayName)).includes(
        "subFlow",
      );
    });

    it("should update execution to a flow", async () => {
      let executions =
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: flowName,
        });
      let execution = executions[executions.length - 1];
      const choice = execution.requirementChoices![1];
      execution.requirement = choice;
      await kcAdminClient.authenticationManagement.updateExecution(
        { flow: flowName },
        execution,
      );

      executions = await kcAdminClient.authenticationManagement.getExecutions({
        flow: flowName,
      });
      execution = executions[executions.length - 1];

      expect(execution.requirement).to.be.eq(choice);
    });

    it("should delete execution", async () => {
      let executions =
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: flowName,
        });
      const id = executions[0].id!;
      await kcAdminClient.authenticationManagement.delExecution({ id });
      executions = await kcAdminClient.authenticationManagement.getExecutions({
        flow: flowName,
      });
      expect(executions.find((ex) => ex.id === id)).to.be.undefined;
    });

    it("should raise priority of execution", async () => {
      let executions =
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: flowName,
        });
      let execution = executions[executions.length - 1];
      const priority = execution.index!;
      await kcAdminClient.authenticationManagement.raisePriorityExecution({
        id: execution.id!,
      });

      executions = await kcAdminClient.authenticationManagement.getExecutions({
        flow: flowName,
      });
      execution = executions.find((ex) => ex.id === execution.id)!;

      expect(execution.index).to.be.eq(priority - 1);
    });

    it("should lower priority of execution", async () => {
      let executions =
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: flowName,
        });
      let execution = executions[0];
      const priority = execution.index!;
      await kcAdminClient.authenticationManagement.lowerPriorityExecution({
        id: execution.id!,
      });

      executions = await kcAdminClient.authenticationManagement.getExecutions({
        flow: flowName,
      });
      execution = executions.find((ex) => ex.id === execution.id)!;

      expect(execution.index).to.be.eq(priority + 1);
    });

    it("should create, update and delete config for execution", async () => {
      const execution = (
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: flowName,
        })
      )[0];
      const alias = "test";
      let config = await kcAdminClient.authenticationManagement.createConfig({
        id: execution.id,
        alias,
      });
      config = await kcAdminClient.authenticationManagement.getConfig({
        id: config.id!,
      });
      expect(config.alias).to.be.eq(alias);

      const extraConfig = { defaultProvider: "sdf" };
      await kcAdminClient.authenticationManagement.updateConfig({
        ...config,
        config: extraConfig,
      });
      config = await kcAdminClient.authenticationManagement.getConfig({
        id: config.id!,
      });

      expect(config.config!.defaultProvider).to.be.eq(
        extraConfig.defaultProvider,
      );

      await kcAdminClient.authenticationManagement.delConfig({
        id: config.id!,
      });
      try {
        await kcAdminClient.authenticationManagement.getConfig({
          id: config.id!,
        });
        fail("should not find deleted config");
      } catch {
        // ignore
      }
    });

    it("should fetch config description for execution", async () => {
      const execution = (
        await kcAdminClient.authenticationManagement.getExecutions({
          flow: flowName,
        })
      )[0];

      const configDescription =
        await kcAdminClient.authenticationManagement.getConfigDescription({
          providerId: execution.providerId!,
        });
      expect(configDescription).is.ok;
      expect(configDescription.providerId).to.be.eq(execution.providerId);
    });
  });
});
