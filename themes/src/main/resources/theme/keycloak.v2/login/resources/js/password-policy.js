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
      value.split("").filter((char) => char === char.toUpperCase() && char !== char.toLowerCase()).length <
      policy.value
    ) {
      return templateError(policy);
    }
  },
  lowerCase: (policy, value) => {
    if (
      value.split("").filter((char) => char === char.toLowerCase() && char !== char.toUpperCase()).length <
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
  for (const p of activePolicies) {
    const validationError = policies[p.name](p.policy, password);
    if (validationError) {
      errors.push(validationError);
    }
  }
  return errors;
}
