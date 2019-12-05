We are excited to have you working on the project and cordially request that you follow the Guidelines:

 - [Got a question or problem?](#question)
 - [You think you've found a bug?](#bug)
 - [Code Style Guidelines](#rules)
 - [Commit Message Guidelines](#commit)
 
## <a name="question"></a> Got a question or problem?
 
 Firstly, please go over our FAQ: https://github.com/angular-ui/ui-select/wiki/FAQs
 
 Please, do not open issues for the general support questions as we want to keep GitHub issues for bug reports and feature requests. You've got much better chances of getting your question answered on [StackOverflow](http://stackoverflow.com/questions/tagged/angular-ui-select) where maintainers are looking at questions tagged with `angular-ui-select`.
 
 StackOverflow is a much better place to ask questions since:
 * there are hundreds of people willing to help on StackOverflow
 * questions and answers stay available for public viewing so your question / answer might help someone else
 * SO voting system assures that the best answers are prominently visible.
 
 To save your and our time we will be systematically closing all the issues that are requests for general support and redirecting people to StackOverflow.
 
## <a name="bug"></a> You think you've found a bug?
 
 Oh, we are ashamed and want to fix it asap! But before fixing a bug we need to reproduce and confirm it. In order to reproduce bugs we will systematically ask you to provide a _minimal_ reproduce scenario using http://plnkr.co/. Having a live reproduce scenario gives us wealth of important information without going back & forth to you with additional questions like:
 * version of AngularJS used
 * version of this library that you are using
 * 3rd-party libraries used, if any
 * and most importantly - a use-case that fails
 
 A minimal reproduce scenario using http://plnkr.co/ allows us to quickly confirm a bug (or point out coding problem) as well as confirm that we are fixing the right problem.
 
 We will be insisting on a minimal reproduce scenario in order to save maintainers time and ultimately be able to fix more bugs. Interestingly, from our experience users often find coding problems themselves while preparing a minimal plunk. We understand that sometimes it might be hard to extract essentials bits of code from a larger code-base but we really need to isolate the problem before we can fix it.
 
 The best part is that you don't need to create plunks from scratch - you can use one from our [demo page](http://plnkr.co/edit/a3KlK8dKH3wwiiksDSn2?p=preview).
 
 Unfortunately we are not able to investigate / fix bugs without a minimal reproduce scenario using http://plnkr.co/, so if we don't hear back from you we are going to close an issue that don't have enough info to be reproduced.
 
## <a name="rules"></a> Coding Rules
To ensure consistency throughout the source code, keep these rules in mind as you are working:

* All features or bug fixes **must pass all tests** (run `gulp` to jshint, build and test).
* All public API methods **must be documented** with ngdoc, an extended version of jsdoc (we added
  support for markdown and templating via @ngdoc tag). To see how we document our APIs, please check
  out the existing ngdocs and see [this wiki page][ngDocs].

**Working on your first Pull Request?** You can learn how from this *free* series [How to Contribute to an Open Source Project on GitHub](https://egghead.io/series/how-to-contribute-to-an-open-source-project-on-github)

## <a name="commit"></a> Git Commit Guidelines

We have very precise rules over how our git commit messages can be formatted for maintenance of the changelog and semvar versioning.  This leads to **more
readable messages** that are easy to follow when looking through the **project history**.  But also,
we use the git commit messages to **generate the change log**.

## Development

### Prepare your environment
* Install [Node.js](http://nodejs.org/) and NPM (should come with)
* Install global dev dependencies: `npm install -g bower gulp`
* Install local dev dependencies: `npm install && bower install` in repository directory

### Development Commands

* `gulp` to jshint, build and test
* `gulp build` to jshint and build
* `gulp test` for one-time test with karma (also build and jshint)
* `gulp watch` to watch src files to jshint, build and test when changed

## Recommended workflow

1. Make changes
2. Run `gulp` or `gulp test` to run Karma tests and ensure they pass.
3. Reset all `dist/*` files
4. Commit changes using the commit message conventions below.
5. Push
6. Create PR

### Commit Message Format
Each commit message consists of a **header**, a **body** and a **footer**.  The header has a special
format that includes a **type**, a **scope** and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The **header** is mandatory and the **scope** of the header is optional.

Any line of the commit message cannot be longer 100 characters! This allows the message to be easier
to read on GitHub as well as in various git tools.

### Revert
If the commit reverts a previous commit, it should begin with `revert: `, followed by the header of the reverted commit. In the body it should say: `This reverts commit <hash>.`, where the hash is the SHA of the commit being reverted.

### Type
Must be one of the following:

* **feat**: A new feature
* **fix**: A bug fix
* **docs**: Documentation only changes
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing
  semi-colons, etc)
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **perf**: A code change that improves performance
* **test**: Adding missing tests
* **chore**: Changes to the build process or auxiliary tools and libraries such as documentation
  generation

### Scope
The scope could be anything specifying place of the commit change. For example `$location`,
`$browser`, `$compile`, `$rootScope`, `ngHref`, `ngClick`, `ngView`, etc...

### Subject
The subject contains succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize first letter
* no dot (.) at the end

### Body
Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes".
The body should include the motivation for the change and contrast this with previous behavior.

### Footer
The footer should contain any information about **Breaking Changes** and is also the place to
reference github issues that this commit **Closes**.

**Breaking Changes** should start with the word `BREAKING CHANGE:` with a space or two newlines. The rest of the commit message is then used for this.

#### Examples

Appears under "Features" header, pencil subheader:

```
feat(pencil): add 'graphiteWidth' option
```

Appears under "Bug Fixes" header, graphite subheader, with a link to issue #GSNP-28:

```
fix(graphite): stop graphite breaking when width < 0.1

Closes #123
```

Appears under "Performance Improvements" header, and under "Breaking Changes" with the breaking change explanation:

```
perf(pencil): remove graphiteWidth option

BREAKING CHANGE: The graphiteWidth option has been removed. The default graphite width of 10mm is always used for performance reason.
```

The following commit and commit `667ecc1` do not appear in the changelog if they are under the same release. If not, the revert commit appears under the "Reverts" header.

```
revert: feat(pencil): add 'graphiteWidth' option

This reverts commit 667ecc1654a317a13331b17617d973392f415f02.
```

A detailed explanation can be found in this [document][https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit#].
