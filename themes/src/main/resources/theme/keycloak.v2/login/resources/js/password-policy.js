const policies = {
  length: (policy, value) => {
    if (value.length < policy.value) {
      return templateError(policy);
    }
  },
  maxLength: (policy, value) => {
    if (value.length > policy.value) {
      return templateError(policy);
    }
  },
  upperCase: (policy, value) => {
    if (
      value.split("").filter((char) => char !== char.toUpperCase()).length >
      policy.value
    ) {
      return templateError(policy);
    }
  },
  lowerCase: (policy, value) => {
    if (
      value.split("").filter((char) => char !== char.toLowerCase()).length >
      policy.value
    ) {
      return templateError(policy);
    }
  },
  digits: (policy, value) => {
    const digits = value.split("").filter((char) => char.match(/\d/));
    if (digits.length < policy.value) {
      return templateError(policy);
    }
  },
  specialChars: (policy, value) => {
    let specialChars = value.split("").filter((char) => char.match(/\W/));
    if (specialChars.length < policy.value) {
      return templateError(policy);
    }
  },
};

const templateError = (policy) => policy.error.replace("{0}", policy.value);

export function validatePassword(password, activePolicies) {
  const errors = [];
  const policiesNames = activePolicies.map((policy) => Object.keys(policy)[0]);
  for (let i = 0; i < policiesNames.length; i++) {
    const policyName = policiesNames[i];
    let policy = policies[policyName];
    let validationError = policy(activePolicies[i][policyName], password);
    if (validationError) {
      errors.push(validationError);
    }
  }
  return errors;
}
