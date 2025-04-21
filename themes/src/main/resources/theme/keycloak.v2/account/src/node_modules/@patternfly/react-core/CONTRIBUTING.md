---
id: Contributing
section: root
hideTOC: true
---

# Contributing to @patternfly/react-core

## Adding a new component

1. Check for open issues that are not assigned to anyone, and assign yourself. If you do not see an issue for the component you want to contribute open an issue and assign yourself. Assigning yourself will ensure that others do not begin working on the component you currently have in progress.
2. Generate the component scaffolding by running `yarn generate`. This will generate a structure that resembles the following
   ```text
   packages/react-core/src/[type]/[ComponentName]/
     index.js - Barrel File exporting public exports
     ComponentName.js - Component Implementation
     ComponentName.test.js - Component Tests
     ComponentName.md - Component Docs
     examples/ - dir for all examples
         SimpleComponentName.js - Simple Example
   ```
3. Write the component implementation in `[Component].js`.
4. Add jest tests to `[Component].test.js`. All new components must be tested.
5. Add any additional public exports to `index.js`
6. Update the generated `[ComponentName].md.` See how to create [component docs.](../react-core/README.md)
7. Add integration tests to the demo-app found [here](../react-integration)

## Code contribution guidelines

Adhering to the following process is the best way to get your work included in the project:

1.  [Fork](https://help.github.com/fork-a-repo/) the project, clone your fork, and configure the remotes:

```bash
# Clone your fork of the repo into the current directory
git clone https://github.com/<your-username>/patternfly-react.git
# Navigate to the newly cloned directory
cd patternfly-react
# Assign the original repo to a remote called "upstream"
git remote add upstream https://github.com/patternfly/patternfly-react.git
```

2.  Create a branch:

```text
$ git checkout -b my-branch -t upstream/main
```

3. Generate your component

```bash
# Run the tool to Generate the component scaffolding
 yarn generate
```

- When you select the option to generate a PatternFly 4 component, a structure resembling the following is generated
  ```text
  packages/react-core/src/[type]/[ComponentName]/
    index.js - Barrel File exporting public exports
    ComponentName.js - Component Implementation
    ComponentName.test.js - Component Tests
    ComponentName.md - Component Docs
  ```

4.  Develop your component. After development is complete, ensure tests and lint standards pass.

```text
$ yarn test
```

Ensure no lint errors are introduced in `yarn-error.log` after running this command.

5.  Add a commit using `yarn commit`:

This project uses [`lerna`](https://lernajs.io/) to do automatic releases and generate a changelog based on the commit history. So we follow [a convention][3] for commit messages. Please follow this convention for your commit messages.

You can use `commitizen` to help you to follow [the convention][3].

Once you are ready to commit the changes, please use the below commands:

```text
$ git add <files to be committed>
$ yarn commit
```

... and follow the instruction of the interactive prompt.

6.  Rebase

Use `git rebase` (not `git merge`) to sync your work from time to time. Ensure all commits related to a single issue have been [squashed](https://github.com/ginatrapani/todo.txt-android/wiki/Squash-All-Commits-Related-to-a-Single-Issue-into-a-Single-Commit).

```text
$ git fetch upstream
$ git rebase upstream/main
```

7.  Push

```text
$ git push origin my-branch
```

8.  Create a pull request

    - A link to the PatternFly 4 demo documentation will be automatically generated and posted as a comment after the pull request build is complete.

## Additional information

See the PatternFly React Guide for full details on [Code Contribution Guidelines](https://github.com/patternfly/patternfly-react/blob/main/CONTRIBUTING.md#code-contribution-guidelines)
