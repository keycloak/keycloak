export class Scope {
  constructor(name, displayName) {
    this.name = name;
    this.displayName = displayName;
  }

  toString() {
    if (this.hasOwnProperty('displayName') && this.displayName) {
      return this.displayName;
    } else {
      return this.name;
    }
  }

}
//# sourceMappingURL=resource-model.js.map