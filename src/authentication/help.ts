export default {
  "authentication-help": {
    name: "Help text for the name of the new flow",
    description: "Help text for the description of the new flow",
    createFlow: "You can create a top level flow within this from",
    flowType: "What kind of form is it",
    topLevelFlowType:
      "What kind of top level flow is it? Type 'client' is used for authentication of clients (applications) when generic is for users and everything else",
    addExecution:
      "Execution can have a wide range of actions, from sending a reset email to validating an OTP",
    addSubFlow:
      "Sub-Flows can be either generic or form. The form type is used to construct a sub-flow that generates a single flow for the user. Sub-flows are a special type of execution that evaluate as successful depending on how the executions they contain evaluate.",
    alias: "Name of the configuration",
  },
};
