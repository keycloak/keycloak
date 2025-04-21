---
id: Release notes
section: developer-resources
releaseNoteTOC: true
---
## 2022.08 release notes (2022-06-24)
Packages released:
- [@patternfly/patternfly@v4.202.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.202.1)

### Components
- **Card:**
  - Added styling to cards when their hidden input is focused ([#4902](https://github.com/patternfly/patternfly/pull/4902))
  - Fixed dark theme card, light bg contrast ([#4903](https://github.com/patternfly/patternfly/pull/4903))
  - Forced dark theme dark section card bg color ([#4913](https://github.com/patternfly/patternfly/pull/4913))
- **Description list:** Added display and card mods ([#4895](https://github.com/patternfly/patternfly/pull/4895))
- **Drawer:** Removed duplicate body element on jumplinks demo ([#4910](https://github.com/patternfly/patternfly/pull/4910))
- **Dropdown:** Added secondary split toggle ([#4897](https://github.com/patternfly/patternfly/pull/4897))
- **Masthead:** Fixed default/dynamic viewport based inset when using resize observer ([#4919](https://github.com/patternfly/patternfly/pull/4919))
- **Menu:** Fixed scrollbar on flyout variant ([#4892](https://github.com/patternfly/patternfly/pull/4892))
- **Menu toggle:** Updated typeahead variant layout/spacing ([#4750](https://github.com/patternfly/patternfly/pull/4750))
- **Notification badge:** Fixed documentation links ([#4891](https://github.com/patternfly/patternfly/pull/4891))
- **Progress stepper:**
  - Added horizontal/vertical breakpoint support ([#4901](https://github.com/patternfly/patternfly/pull/4901))
  - Used button for step title help text ([#4912](https://github.com/patternfly/patternfly/pull/4912))
- **Table:**
  - Added tr border to sticky nested table header ([#4857](https://github.com/patternfly/patternfly/pull/4857))
  - Removed bullseye layout in demos ([#4904](https://github.com/patternfly/patternfly/pull/4904))
- **Tabs:** Added expandable overflow styling ([#4876](https://github.com/patternfly/patternfly/pull/4876))
- **Page:** Added height breakpoints for sticky top and bottom modifiers ([#4905](https://github.com/patternfly/patternfly/pull/4905))
- **Wizard:** Moved footer to sibling of drawer ([#4896](https://github.com/patternfly/patternfly/pull/4896))


## 2022.07 release notes (2022-06-03)
Packages released:
- [@patternfly/patternfly@v4.196.7](https://www.npmjs.com/package/@patternfly/patternfly/v/4.196.7)

### Components
- **Calendar month:** Improved layout of nav controls ([#4862](https://github.com/patternfly/patternfly/pull/4862))
- **Chip group:** Fixed premature wrapping ([#4879](https://github.com/patternfly/patternfly/pull/4879))
- **Drawer:** Added demos with jumplinks ([#4608](https://github.com/patternfly/patternfly/pull/4608))
- **Form field:** Documented autocomplete limitation ([#4856](https://github.com/patternfly/patternfly/pull/4856))
- **Form:** Added complex form demo ([#4865](https://github.com/patternfly/patternfly/pull/4865))
- **Label:** Added gold, updated orange bgcolor ([#4863](https://github.com/patternfly/patternfly/pull/4863))
- **Page:** Added masthead to header/nav/main stacking context order ([#4839](https://github.com/patternfly/patternfly/pull/4839))
- **Table:**
  - Added expanded set column width example ([#4852](https://github.com/patternfly/patternfly/pull/4852))
  - Added image and text demo ([#4853](https://github.com/patternfly/patternfly/pull/4853))

### Other
- **Theme:**
  * Imported dark theme per component, added new global var ([#4864](https://github.com/patternfly/patternfly/pull/4864))
  * Updated dark theme default text color ([#4875](https://github.com/patternfly/patternfly/pull/4875))
  * Updated build process to copy files from new SCSS location ([#4878](https://github.com/patternfly/patternfly/issues/4878))
- **Utilities:** Added font family utilities ([#4868](https://github.com/patternfly/patternfly/pull/4868))


## 2022.06 release notes (2022-05-13)
Packages released:
- [@patternfly/patternfly@v4.194.4](https://www.npmjs.com/package/@patternfly/patternfly/v/4.194.4)

### Components
- **Chip group:** Fixed an overflow problem ([#4836](https://github.com/patternfly/patternfly/pull/4836))
- **Description list:** Fixed text wrap ([#4809](https://github.com/patternfly/patternfly/pull/4809))
- **Label:** Added button for adding new labels ([#4828](https://github.com/patternfly/patternfly/pull/4828))
- **Menu:** Removed phantom scrollbar in drilldown menu ([#4807](https://github.com/patternfly/patternfly/pull/4807))
- **Spinner:** Reversed the spin of legacy spinner ([#4830](https://github.com/patternfly/patternfly/pull/4830))
- **Tabs:**
  - Fixed demo typo ([#4808](https://github.com/patternfly/patternfly/pull/4808))
  - Fixed add button focus outline ([#4820](https://github.com/patternfly/patternfly/pull/4820))
  - Set disabled tab text color ([#4829](https://github.com/patternfly/patternfly/pull/4829))
- **Tooltip:** Added dark theme border ([#4840](https://github.com/patternfly/patternfly/pull/4840))
- **Tree view:** Added z-index to prevent guide from disappearing on focus ([#4813](https://github.com/patternfly/patternfly/pull/4813))

### Other
- **Charts:**
  - Added dark theme support ([#4815](https://github.com/patternfly/patternfly/pull/4815))
  - Removed workspace style ([#4834](https://github.com/patternfly/patternfly/pull/4834))
  - Added dark theme tooltip border styles ([#4842](https://github.com/patternfly/patternfly/pull/4842))
- **Deps:** Updated dependency theme-patternfly-org to v0.11.32 ([#4410](https://github.com/patternfly/patternfly/pull/4410))
- **Primary detail:** Updated card view demo to use new selectable styles ([#4810](https://github.com/patternfly/patternfly/pull/4810))

## 2022.05 release notes (2022-04-20)
Packages released:
- [@patternfly/patternfly@v4.192.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.192.1)

### Components
- **Card:** Fixed stacking context issue with selectable raised cards ([#4780](https://github.com/patternfly/patternfly/pull/4780))
- **Data list:** Updated expandable demo to include expand/collapse all in toolbar ([#4784](https://github.com/patternfly/patternfly/pull/4784))
- **Divider:** Added horizontal/vertical breakpoint support to divider ([#4765](https://github.com/patternfly/patternfly/pull/4765))
- **Form control:** Fixed placeholder variation menu item color ([#4773](https://github.com/patternfly/patternfly/pull/4773))
- **Form:**
  - Added CSS vars for defining label cursor ([#4779](https://github.com/patternfly/patternfly/pull/4779))
  - Changed width limited form max width from 500 to 800px ([#4782](https://github.com/patternfly/patternfly/pull/4782))
- **Login:** Added support for any language selector menu ([#4793](https://github.com/patternfly/patternfly/pull/4793))
- **Page:** Updated dark theme main section bgcolor ([#4791](https://github.com/patternfly/patternfly/pull/4791))
- **Search input:** Converted to text-input-group ([#4730](https://github.com/patternfly/patternfly/pull/4730))
- **Switch:** Updated to switch state colors, added checked + label example ([#4766](https://github.com/patternfly/patternfly/pull/4766))
- **Tabs:**
  - Added secondary border-bottom variation, update demos ([#4774](https://github.com/patternfly/patternfly/pull/4774))
  - Added close button and add new tab ([#4787](https://github.com/patternfly/patternfly/pull/4787))
- **Wizard:** Added drawer example ([#4778](https://github.com/patternfly/patternfly/pull/4778))

### Other
- **Global:** Updated code and pre elements to use PF mono font stack ([#4783](https://github.com/patternfly/patternfly/pull/4783))
- **Theme:** Added stylesheet for prefers-color-scheme dark ([#4761](https://github.com/patternfly/patternfly/pull/4761))


## 2022.04 release notes (2022-03-30)
Packages released:
- [@patternfly/patternfly@v4.185.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.185.1)

### Components
- **Accordion:** Made links at bottom of bordered item clickable ([#4740](https://github.com/patternfly/patternfly/pull/4740))
- **Description list:** Fixed var typo in docs ([#4739](https://github.com/patternfly/patternfly/pull/4739))
- **Form:** Improved accessible label on form elements in examples/demos ([#4714](https://github.com/patternfly/patternfly/pull/4714))
- **Form control:** Updated invalid sprite css, examples ([#4732](https://github.com/patternfly/patternfly/pull/4732))
- **Menu toggle:**
  * Added typeahead variation ([#4673](https://github.com/patternfly/patternfly/pull/4673))
  * Added example of secondary with icon ([#4743](https://github.com/patternfly/patternfly/pull/4743))
- **Page/modal/wizard:** Made scrollable regions keyboard focusable ([#4736](https://github.com/patternfly/patternfly/pull/4736))
- **Pagination:** Updated options menu toggle so whole toggle is clickable ([#4723](https://github.com/patternfly/patternfly/pull/4723))

### Other
- **Build:** Updated build to include themes directory ([#4728](https://github.com/patternfly/patternfly/pull/4728))
- **Dark theme:** Fixed dark theme shadow pf-size-prem ([#4752](https://github.com/patternfly/patternfly/pull/4752))
- **Demos:** Added new page template to demos ([#4741](https://github.com/patternfly/patternfly/pull/4741))
- **Icons:** Added critical-risk pficon ([#4758](https://github.com/patternfly/patternfly/pull/4758))


## 2022.03 release notes (2022-03-08)
Packages released:
- [@patternfly/patternfly@v4.183.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.183.1)

### Components
- **Description list:** Added description list demo ([#4715](https://github.com/patternfly/patternfly/pull/4715))
- **Form control:** Added icon sprite variation ([#4711](https://github.com/patternfly/patternfly/pull/4711))
- **Jump links:** Fixed outdated code in demos ([#4703](https://github.com/patternfly/patternfly/pull/4703))
- **Label group:** Added compact examples ([#4639](https://github.com/patternfly/patternfly/pull/4639))
- **Masthead:** Updated toggle borders ([#4706](https://github.com/patternfly/patternfly/pull/4706))
- **Menu:**
  - Added aria attributes ([#4670](https://github.com/patternfly/patternfly/pull/4670))
  - Added checkbox to menu ([#4696](https://github.com/patternfly/patternfly/pull/4696))
  - Added image support ([#4701](https://github.com/patternfly/patternfly/pull/4701))
- **Menu toggle:**
  - Fixed plain menu toggle state ([#4710](https://github.com/patternfly/patternfly/pull/4710))
  - Added split button ([#4713](https://github.com/patternfly/patternfly/pull/4713))
  - Fixed self referencing var ([#4727](https://github.com/patternfly/patternfly/pull/4727))
- **Page:** Updated wording in centered examples ([#4698](https://github.com/patternfly/patternfly/pull/4698))
- **Select:** Fixed active focus states bottom border ([#4702](https://github.com/patternfly/patternfly/pull/4702))
- **Spinner:** Renamed examples so SVG is default, non-SVG is legacy ([#4697](https://github.com/patternfly/patternfly/pull/4697))
- **Table:**
  - Added docs around z-index conflicts with use of sticky columns/headers ([#4709](https://github.com/patternfly/patternfly/pull/4709))
  - Updated overflow menu examples ([#4719](https://github.com/patternfly/patternfly/pull/4719))
- **Tabs:** Updated tabs demos to use secondary tabs and standard styles ([#4712](https://github.com/patternfly/patternfly/pull/4712))
- **Wizard:** Updated docs/hbs for description to allow div ([#4708](https://github.com/patternfly/patternfly/pull/4708))

### Other
- **Demos:**
  - Used search inputs in demos ([#4691](https://github.com/patternfly/patternfly/pull/4691))
  - Added dashboard demo ([#4721](https://github.com/patternfly/patternfly/pull/4721))


## 2022.02 release notes (2022-02-17)
Packages released:
- [@patternfly/patternfly@v4.179.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.179.1)

### Components
- **Alert:** Added overflow alert button ([#4650](https://github.com/patternfly/patternfly/pull/4650))
- **Avatar:** Added size variations ([#4648](https://github.com/patternfly/patternfly/pull/4648))
- **Button:** Fixed primary toggle state styles ([#4662](https://github.com/patternfly/patternfly/pull/4662))
- **Card:** Updated card demo select to be plain, fixed alignment ([#4667](https://github.com/patternfly/patternfly/pull/4667))
- **Code editor:** Added header content section ([#4669](https://github.com/patternfly/patternfly/pull/4669))
- **Label:** Improved click area of editable label ([#4653](https://github.com/patternfly/patternfly/pull/4653))
- **Nav:**
  - Added variation to fix section spacing ([#4649](https://github.com/patternfly/patternfly/pull/4649))
  - Updated nav menu styling ([#4672](https://github.com/patternfly/patternfly/pull/4672))
- **Pagination:** Removed number input arrow visibility ([#4641](https://github.com/patternfly/patternfly/pull/4641))
- **Progress stepper:** Removed extra space below last step ([#4636](https://github.com/patternfly/patternfly/pull/4636))
- **Table:**
  - Updated tree table checkbox padding ([#4642](https://github.com/patternfly/patternfly/pull/4642))
  - Removed unnecessary class from striped tr example ([#4644](https://github.com/patternfly/patternfly/pull/4644))
  - Added overflow menu examples ([#4651](https://github.com/patternfly/patternfly/pull/4651))
  - Fixed expand all button in compact table ([#4681](https://github.com/patternfly/patternfly/pull/4681))
- **Text input group:** Added placeholder styling ([#4668](https://github.com/patternfly/patternfly/pull/4668))

### Other
- **Demos:**
  - Removed bulk selector from demo headers ([#4640](https://github.com/patternfly/patternfly/pull/4640))
  - Created common template for full page demos ([#4674](https://github.com/patternfly/patternfly/pull/4674))
  - Added common masthead template ([#4683](https://github.com/patternfly/patternfly/pull/4683))
  - Fixed context selector include paths ([#4689](https://github.com/patternfly/patternfly/pull/4689))
- **Docs:**
  - Added links to new breakpoint section ([#4675](https://github.com/patternfly/patternfly/pull/4675))
  - Added version details to upgrade guide ([#4678](https://github.com/patternfly/patternfly/pull/4678))
- **Repo:** Added bug and feature issue templates ([#4671](https://github.com/patternfly/patternfly/pull/4671))
- **Theme:** Added dark theme ([#4654](https://github.com/patternfly/patternfly/pull/4654))
  - **Note:** This is not production ready and we are not advising products to adopt the dark theme stylesheet at this time. This feature is being released to test in specific use cases to assess the feasibility of adding a dark theme to PatternFly.


## 2022.01 release notes (2022-01-25)
Packages released:
- [@patternfly/patternfly@v4.171.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.171.1)

### Components
- **Button:** Added progress support with plain variation ([#4594](https://github.com/patternfly/patternfly/pull/4594))
- **Context selector, dropdown, menu toggle, options menu, select:** Added hover state for all plain variant icons ([#4627](https://github.com/patternfly/patternfly/pull/4627))
- **Description list:**
  - Removed unnecessary column fill examples ([#4593](https://github.com/patternfly/patternfly/pull/4593))
  - Added icon variant ([#4603](https://github.com/patternfly/patternfly/pull/4603))
- **Expandable section:** Added indented variation ([#4571](https://github.com/patternfly/patternfly/pull/4571))
- **Label:** Adjusted style of editable label ([#4551](https://github.com/patternfly/patternfly/pull/4551))
- **Log viewer:**
  - Updated scroll, added footer ([#4587](https://github.com/patternfly/patternfly/pull/4587))
  - Updated the way dark theme is applied, adjusted border ([#4597](https://github.com/patternfly/patternfly/pull/4597))
- **Login page:** Aligned login box to top ([#4591](https://github.com/patternfly/patternfly/pull/4591))
- **Masthead:**
  - Added horizontal nav demo ([#4617](https://github.com/patternfly/patternfly/pull/4617))
  - Added resize observer conditional ([#4625](https://github.com/patternfly/patternfly/pull/4625))
- **Notification badge:** Changed example to dark ([#4580](https://github.com/patternfly/patternfly/pull/4580))
- **Table:**
  - Added striped rows ([#4569](https://github.com/patternfly/patternfly/pull/4569))
  - Fixed sort/fit-content width ([#4589](https://github.com/patternfly/patternfly/pull/4589))
  - Moved expand all toggle in demo ([#4595](https://github.com/patternfly/patternfly/pull/4595))
  - Removed cols/colgroups ([#4600](https://github.com/patternfly/patternfly/pull/4600))
- **Text input group:** Added autocomplete ghosting ([#4616](https://github.com/patternfly/patternfly/pull/4616))
- **Truncate:**
  - Resolved spacing issues ([#4599](https://github.com/patternfly/patternfly/pull/4599))
  - Fixed font size var value ([#4560](https://github.com/patternfly/patternfly/pull/4560))

### Other
- **Docs:**
  - Promoted beta components ([#4613](https://github.com/patternfly/patternfly/pull/4613))
  - Removed outdated modifiers page ([#4619](https://github.com/patternfly/patternfly/pull/4619))
- **Global:** Updated/removed unused/undefined vars ([#4620](https://github.com/patternfly/patternfly/pull/4620))
- **Build:**
  - Cleaned up stylelint rules/sass ([#4567](https://github.com/patternfly/patternfly/pull/4567))
  - Made reloading on change to markdown files work again ([#4574](https://github.com/patternfly/patternfly/pull/4574))
  - Updated the watcher to handle crashes ([#4583](https://github.com/patternfly/patternfly/pull/4583))


## 2021.16 release notes (2021-12-07)
Packages released:
- [@patternfly/patternfly@v4.164.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.164.2)

### Components
- **Button:** Added demo for progress button completion state ([#4528](https://github.com/patternfly/patternfly/pull/4528))
- **Card:** Replaced dropdowns with selects in demos ([#4520](https://github.com/patternfly/patternfly/pull/4520))
- **Description list:** Added columnar layout ([#4544](https://github.com/patternfly/patternfly/pull/4544))
- **Dropdown:**
  - Added plain text variant ([#4542](https://github.com/patternfly/patternfly/pull/4542))
  - Added disabled styles ([#4543](https://github.com/patternfly/patternfly/pull/4543))
- **Form:** Added support for horizontal layout at various breakpoints ([#4536](https://github.com/patternfly/patternfly/pull/4536))
- **Multiple file upload:** Added component ([#4548](https://github.com/patternfly/patternfly/pull/4548))
- **Table:**
  - Removed cursor pointer on disabled checkboxes ([#4527](https://github.com/patternfly/patternfly/pull/4527))
  - Removed extra padding from nested table cells ([#4529](https://github.com/patternfly/patternfly/pull/4529))
  - Reorganized documentation ([#4539](https://github.com/patternfly/patternfly/pull/4539))
  - Increased target area of checkboxes/radios ([#4546](https://github.com/patternfly/patternfly/pull/4546))
- **Toolbar:** Set labels to show in collapsed state ([#4451](https://github.com/patternfly/patternfly/pull/4451))
- **Truncate:**
  - Added truncate component ([#4502](https://github.com/patternfly/patternfly/pull/4502))
  - Fixed font size var value ([#4560](https://github.com/patternfly/patternfly/pull/4560))

### Other
- **Build:**
  - Added a11y coverage report action ([#4530](https://github.com/patternfly/patternfly/pull/4530))
  - Updated stylelint, deps ([#4537](https://github.com/patternfly/patternfly/pull/4537))
- **Demos:** Added a password generator demo ([#4531](https://github.com/patternfly/patternfly/pull/4531))
- **Fonts:** Added opt-ins for new red hat font ([#4476](https://github.com/patternfly/patternfly/pull/4476))
- **Global:** Removed custom firefox focus styles ([#4533](https://github.com/patternfly/patternfly/pull/4533))

## 2021.15 release notes (2021-11-16)
Packages released:
- [@patternfly/patternfly@v4.159.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.159.1)

### Components
- **Card:** Added non-selectable card, updated card view demo ([#4500](https://github.com/patternfly/patternfly/pull/4500))
- **Dropdown:**
  - Added secondary variant to dropdown ([#4498](https://github.com/patternfly/patternfly/pull/4498))
  - Applied primary styling to split ([#4508](https://github.com/patternfly/patternfly/pull/4508))
- **Dual list selector:** Aligned html/a11y with react ([#4499](https://github.com/patternfly/patternfly/pull/4499))
- **Form select:** Matched padding with select ([#4490](https://github.com/patternfly/patternfly/pull/4490))
- **Log viewer:**
  - Added nowrap variation ([#4455](https://github.com/patternfly/patternfly/pull/4455))
  - Updated the way dark theme is applied ([#4506](https://github.com/patternfly/patternfly/pull/4506))
- **Menu toggle:**
  - Added plain w/text variant ([#4491](https://github.com/patternfly/patternfly/pull/4491))
  - Added rounded corners to primary ([#4494](https://github.com/patternfly/patternfly/pull/4494))
- **Menu:** Added nav variant ([#4513](https://github.com/patternfly/patternfly/pull/4513))
- **Nav:**
  - Added drilldown menu to nav ([#4458](https://github.com/patternfly/patternfly/pull/4458))
  - Fixed toggle icon rotation ([#4486](https://github.com/patternfly/patternfly/pull/4486))
- **Options menu:** Moved text into button in plain text variant ([#4492](https://github.com/patternfly/patternfly/pull/4492))
- **Table:** Fixed pf-m-truncate alignment ([#4489](https://github.com/patternfly/patternfly/pull/4489))
- **TextInputGroup:** Added disabled styling ([#4484](https://github.com/patternfly/patternfly/pull/4484))
- **Tooltip:** Added support for diagonal positioning ([#4470](https://github.com/patternfly/patternfly/pull/4470))

### Other
- **README:** Updated a11y docs, s/npm run/yarn ([#4496](https://github.com/patternfly/patternfly/pull/4496))


## 2021.14 release notes (2021-10-26)
Packages released:
- [@patternfly/patternfly@v4.151.4](https://www.npmjs.com/package/@patternfly/patternfly/v/4.151.4)

### Components
- **App launcher, menu, select:** Fixed favorite colors ([#4437](https://github.com/patternfly/patternfly/pull/4437))
- **Card:** Added hoverable/selectable/selected-raised ([#4425](https://github.com/patternfly/patternfly/pull/4425))
- **Context selector:** Added demos ([#4454](https://github.com/patternfly/patternfly/pull/4454))
- **Form:** Added group role to section and field group ([#4424](https://github.com/patternfly/patternfly/pull/4424))
- **Modal:** Added demo to link form to submit button in footer ([#4432](https://github.com/patternfly/patternfly/pull/4432))
- **Nav:**
  - Added third level expansion ([#4460](https://github.com/patternfly/patternfly/pull/4460))
  - Removed broken icon font size var ([#4473](https://github.com/patternfly/patternfly/pull/4473))
- **Pagination:** Added indeterminate count example ([#4428](https://github.com/patternfly/patternfly/pull/4428))
- **Panel:** Added panel component ([#4456](https://github.com/patternfly/patternfly/pull/4456))
- **Popover:**
  - Added alert styling ([#4452](https://github.com/patternfly/patternfly/pull/4452))
  - Increased box shadow from medium to large ([#4457](https://github.com/patternfly/patternfly/pull/4457))
- **Select:** Added vars for width/min-width ([#4443](https://github.com/patternfly/patternfly/pull/4443))
- **Table:** Added nested headers ([#4448](https://github.com/patternfly/patternfly/pull/4448))
- **Text input group:** Moved icon modifier to main element ([#4465](https://github.com/patternfly/patternfly/pull/4465))
- **Tile:** Updated hover/selected styles ([#4439](https://github.com/patternfly/patternfly/pull/4439))
- **Toolbar:** Renamed sticky-top to sticky ([#4461](https://github.com/patternfly/patternfly/pull/4461))
- **Tooltip:** Updated spacing, arrow ([#4462](https://github.com/patternfly/patternfly/pull/4462))

### Other
- **Build:** Fix select border minification ([#4442](https://github.com/patternfly/patternfly/pull/4442))
- **Docs:**
  - Promote beta components ([#4459](https://github.com/patternfly/patternfly/pull/4459))
  - Renamed drag drop to drag and drop in sidebar ([#4450](https://github.com/patternfly/patternfly/pull/4450))
- **Global:** Updated use of date element in examples (#4412) ([#4423](https://github.com/patternfly/patternfly/pull/4423))
- **Icons:**
  - Copied unicodes from PF3 icons, make new icon unicodes persist ([#4402](https://github.com/patternfly/patternfly/pull/4402))
  - Moved unicodes json write to src so it's bundled with build ([#4468](https://github.com/patternfly/patternfly/pull/4468))


## 2021.13 release notes (2021-10-13)
Packages released:
- [@patternfly/patternfly@v4.144.5](https://www.npmjs.com/package/@patternfly/patternfly/v/4.144.5)

### Components
- **Backdrop:** Added var for position to allow customization ([#4391](https://github.com/patternfly/patternfly/pull/4391))
- **Banner:** Added link style ([#4383](https://github.com/patternfly/patternfly/pull/4383))
- **Card:**
  * Updated log view and event view demos ([#4371](https://github.com/patternfly/patternfly/pull/4371))
  * Fixed demo image paths ([#4400](https://github.com/patternfly/patternfly/pull/4400))
- **Drag drop:**
  * Added drag drop ([#4398](https://github.com/patternfly/patternfly/pull/4398))
  * Moved drag drop styles into component ([#4404](https://github.com/patternfly/patternfly/pull/4404))
- **Dual list selector:** Added support for disabled items ([#4361](https://github.com/patternfly/patternfly/pull/4361))
- **Jump links:** Moved text into expandable toggle ([#4352](https://github.com/patternfly/patternfly/pull/4352))
- **Label:** Added compact variant ([#4386](https://github.com/patternfly/patternfly/pull/4386))
- **Log viewer:**
  * Preserved white space in log viewer text ([#4397](https://github.com/patternfly/patternfly/pull/4397))
  * Made text fill available space in container ([#4406](https://github.com/patternfly/patternfly/pull/4406))
- **Masthead:** Updated column-end ([#4418](https://github.com/patternfly/patternfly/pull/4418))
- **Masthead, description list**: Fixed undefined vars ([#4421](https://github.com/patternfly/patternfly/pull/4421))
- **Menu:** Added scrollable and plain modifiers ([#4392](https://github.com/patternfly/patternfly/pull/4392))
- **Nav:**
  - Added support for menu component as flyout ([#4417](https://github.com/patternfly/patternfly/pull/4417))
  - Marked nav flyout example as beta, updated release notes ([#4444](https://github.com/patternfly/patternfly/pull/4444))
- **Progress stepper:** Added help text for popover ([#4381](https://github.com/patternfly/patternfly/pull/4381))
- **Radio, checkbox, form:** Corrected label alignment ([#4375](https://github.com/patternfly/patternfly/pull/4375))
- **Select:**
  * Added support for placeholder in toggle ([#4377](https://github.com/patternfly/patternfly/pull/4377))
  * Removed double invalid icon on invalid typeahead ([#4385](https://github.com/patternfly/patternfly/pull/4385))
- **Table:**
  * Added fixed columns ([#4326](https://github.com/patternfly/patternfly/pull/4326))
  * Addressed followup compact tree issues ([#4389](https://github.com/patternfly/patternfly/pull/4389))
- **Text input group:** Added component ([#4408](https://github.com/patternfly/patternfly/pull/4408))
- **Toolbar:** Adjusted demo heights, removed dupe demo ([#4373](https://github.com/patternfly/patternfly/pull/4373))

### Other
- **Deps:** Update dependency theme-patternfly-org to v0.7.3 ([#4275](https://github.com/patternfly/patternfly/pull/4275))
- **Utilities:** Added breakpoint options to docs ([#4409](https://github.com/patternfly/patternfly/pull/4409))


## 2021.12 release notes (2021-09-14)
Packages released:
- [@patternfly/patternfly@v4.135.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.135.2)

### Components
- **Dual list selector:** Added drag/drop ([#4356](https://github.com/patternfly/patternfly/pull/4356))
- **Form control:** Removed extra space under textarea ([#4329](https://github.com/patternfly/patternfly/pull/4329))
- **Log viewer:** Updated to support dynamic size list ([#4327](https://github.com/patternfly/patternfly/pull/4327))
- **Masthead:** Added content wrapper to demo ([#4325](https://github.com/patternfly/patternfly/pull/4325))
- **Nav:**
  - Added flyout menus ([#4301](https://github.com/patternfly/patternfly/pull/4301))
  - Added drilldown variant ([#4364](https://github.com/patternfly/patternfly/pull/4364))
- **Popover/modal:** Updated dialog role usage/docs ([#4363](https://github.com/patternfly/patternfly/pull/4363))
- **Progress stepper:**
  - Added component ([#4357](https://github.com/patternfly/patternfly/pull/4357))
  - Fixed global color var name ([#4367](https://github.com/patternfly/patternfly/pull/4367))
- **Tabs:** Moved text into expandable toggle ([#4333](https://github.com/patternfly/patternfly/pull/4333))

### Other
- **A11y:** Enabled ignored checks, fixed examples ([#4324](https://github.com/patternfly/patternfly/pull/4324))
- **Build:** Updated to use yarn ([#4334](https://github.com/patternfly/patternfly/pull/4334))
- **Ci:**
  - Converted circleci to github actions ([#4342](https://github.com/patternfly/patternfly/pull/4342))
  - Added actions followup ([#4344](https://github.com/patternfly/patternfly/pull/4344))
  - Uploaded pf4.patternfly.org ([#4346](https://github.com/patternfly/patternfly/pull/4346))
  - Tested breaking change lint ([#4348](https://github.com/patternfly/patternfly/pull/4348))
  - Removed breaking change lint ([#4350](https://github.com/patternfly/patternfly/pull/4350))
- **Deps:** Updated dependency @patternfly/patternfly-a11y to v4.2.1 ([#4309](https://github.com/patternfly/patternfly/pull/4309))
- **Docs:**
  - Fixed broken links ([#4341](https://github.com/patternfly/patternfly/pull/4341))
  - Update some documentation links to be relative ([#4347](https://github.com/patternfly/patternfly/pull/4347))


## 2021.11 release notes (2021-08-24)
Packages released:
- [@patternfly/patternfly@v4.132.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.132.2)

### Components
- **Alert:**
  - Added plain inline alert ([#4262](https://github.com/patternfly/patternfly/pull/4262))
  - Added expandable variation ([#4268](https://github.com/patternfly/patternfly/pull/4268))
- **Back to top:** Added back to top component ([#4291](https://github.com/patternfly/patternfly/pull/4291))
- **Card:** Linked a url in detail card demo ([#4311](https://github.com/patternfly/patternfly/pull/4311))
- **Clipboard copy:** Allowed gap b/w wrapping lines in inline variation ([#4296](https://github.com/patternfly/patternfly/pull/4296))
- **Description list:** Updated spacing, added compact and fluid ([#4243](https://github.com/patternfly/patternfly/pull/4243))
- **Log viewer:** Removed border from log viewer in dark theme ([#4316](https://github.com/patternfly/patternfly/pull/4316))
- **Masthead:**
  - Added inset support ([#4295](https://github.com/patternfly/patternfly/pull/4295))
  - Added toolbar support ([#4211](https://github.com/patternfly/patternfly/pull/4211))
- **Menu:** Scoped menu component styles in dropdown ([#4314](https://github.com/patternfly/patternfly/pull/4314))
- **Number input:** Adjusted alignment ([#4260](https://github.com/patternfly/patternfly/pull/4260))
- **Table:** Added mobile sort to sortable demo ([#4307](https://github.com/patternfly/patternfly/pull/4307))
- **Tabs:** Added disabled/aria-disabled support ([#4278](https://github.com/patternfly/patternfly/pull/4278))
- **Tree view:** Added compact, bordered, and background transparent variants ([#4242](https://github.com/patternfly/patternfly/pull/4242))

### Other
- **Build**
  - Bump theme-patternfly-org and enable renovatebot ([#4267](https://github.com/patternfly/patternfly/pull/4267))
  - Pin dependencies ([#4269](https://github.com/patternfly/patternfly/pull/4269))
  - Update dependency theme-patternfly-org to v0.6.14 ([#4270](https://github.com/patternfly/patternfly/pull/4270))
  - Update dependency @patternfly/patternfly-a11y to v4 ([#4273](https://github.com/patternfly/patternfly/pull/4273))
- **Docs:**
  - Updated example/demo microcopy ([#4285](https://github.com/patternfly/patternfly/pull/4285))
  - Added extensions to workspace nav, move log viewer to extensions ([#4286](https://github.com/patternfly/patternfly/pull/4286))
  - Update tabs and password strength docs ([#4300](https://github.com/patternfly/patternfly/pull/4300))
  - Removed beta tag from promoted components ([#4305](https://github.com/patternfly/patternfly/pull/4305))
- **Global:** Defined previously undefined vars ([#4261](https://github.com/patternfly/patternfly/pull/4261))
- **Layouts:** Added split wrappable demo ([#4304](https://github.com/patternfly/patternfly/pull/4304))
- **Surge:** Replace master with main ([#4259](https://github.com/patternfly/patternfly/pull/4259))
- **Utilities:** Updated text and background utilities ([#4292](https://github.com/patternfly/patternfly/pull/4292))


## 2021.10 release notes (2021-08-03)
Packages released:
- [@patternfly/patternfly@v4.125.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.125.2)

### Components
- **Button:** Updated accessibility table ([#4200](https://github.com/patternfly/patternfly/pull/4200))
- **Context selector/dropdown:**
  * Changed full height toggle display type ([#4179](https://github.com/patternfly/patternfly/pull/4179))
  * Moved `::before` values to `::after` ([#4182](https://github.com/patternfly/patternfly/pull/4182))
  * Reverted move `::before` values to `::after` ([#4213](https://github.com/patternfly/patternfly/pull/4213))
- **Form:** Added info text to group label ([#4172](https://github.com/patternfly/patternfly/pull/4172))
- **Form group:** Updated form group roles to support checkbox/radio groups ([#4240](https://github.com/patternfly/patternfly/pull/4240))
- **Helper text:** Updated static and dynamic to use same icons ([#4246](https://github.com/patternfly/patternfly/pull/4246))
- **Jump links:** Forced vertical layout with expandable ([#4230](https://github.com/patternfly/patternfly/pull/4230))
- **Login:** Added a password strength demo ([#4145](https://github.com/patternfly/patternfly/pull/4145))
- **Menu:**
  * Added vars for menu top, left, and right position offset ([#4192](https://github.com/patternfly/patternfly/pull/4192))
  * Moved menu position modifers to menu element ([#4199](https://github.com/patternfly/patternfly/pull/4199))
- **Menu toggle:**
  * Added full height variant ([#4153](https://github.com/patternfly/patternfly/pull/4153))
  * Truncated overflow text ([#4236](https://github.com/patternfly/patternfly/pull/4236))
- **Nav:** Added horizontal subnav ([#4229](https://github.com/patternfly/patternfly/pull/4229))
- **Notification badge:** Updated examples to show tasks icon ([#4241](https://github.com/patternfly/patternfly/pull/4241))
- **Popover:** Added more position options to position arrow ([#4226](https://github.com/patternfly/patternfly/pull/4226))
- **Search input:** Added variant with submit button ([#4180](https://github.com/patternfly/patternfly/pull/4180))
- **Spinner:** Hid overflow from rotating elements ([#4208](https://github.com/patternfly/patternfly/pull/4208))
- **Switch:** Added reverse layout ([#4235](https://github.com/patternfly/patternfly/pull/4235))
- **Table:** Cleaned up docs structure ([#4215](https://github.com/patternfly/patternfly/pull/4215))
- **Toolbar:** Fixed hidden/visible mods ([#4197](https://github.com/patternfly/patternfly/pull/4197))
- **Wizard:** Switched ol to span in wizard toggle ([#4237](https://github.com/patternfly/patternfly/pull/4237))

### Other
- **Ci:**
  * Renamed master to main ([#4194](https://github.com/patternfly/patternfly/pull/4194))
  * Updated scripts to use main instead of master ([#4195](https://github.com/patternfly/patternfly/pull/4195))
- **Icons:**
  * Added styles to represent different SVG icon sizes and alignment for patternfly-react ([#3871](https://github.com/patternfly/patternfly/pull/3871))
  * Added task icon to pficons ([#4184](https://github.com/patternfly/patternfly/pull/4184))
  * Added instructions to add icon to pficon font ([#4221](https://github.com/patternfly/patternfly/pull/4221))
- **Utilities:** Added min/max height and width to sizing utility ([#4009](https://github.com/patternfly/patternfly/pull/4009))
- **Fonts:**
  * Dropped '?#iefix' for font face definitions ([#4209](https://github.com/patternfly/patternfly/pull/4209))
  * Dropped support for legacy fonts ([#4210](https://github.com/patternfly/patternfly/pull/4210))
- **Docs:** Updated references to Kitty Giraudel's deadname ([#4239](https://github.com/patternfly/patternfly/pull/4239))


## 2021.08 release notes (2021-06-22)
Packages released:
- [@patternfly/patternfly@v4.115.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.115.2)

### Components
- **Content:** Changed visited to declare color property ([#4132](https://github.com/patternfly/patternfly/pull/4132))
- **Context selector:** Added plain text variant and example ([#4095](https://github.com/patternfly/patternfly/pull/4095))
- **Context selector and dropdown:**
  - Added dropdown, context selector full height mods ([#4108](https://github.com/patternfly/patternfly/pull/4108))
  - Fixed full height border active, focus ([#4154](https://github.com/patternfly/patternfly/pull/4154))
- **Expandable section:** Added large variation ([#4143](https://github.com/patternfly/patternfly/pull/4143))
- **Form:** Added missing padding to labels in collapsed horizontal mode ([#4152](https://github.com/patternfly/patternfly/pull/4152))
- **Helper text:**
  - Renamed invalid state to error ([#4140](https://github.com/patternfly/patternfly/pull/4140))
  - Added demo ([#4162](https://github.com/patternfly/patternfly/pull/4162))
- **Label:** Replaced "overflow" text with "truncate" in truncation examples ([#4136](https://github.com/patternfly/patternfly/pull/4136))
- **Log viewer:**
  - Added dark variation ([#4133](https://github.com/patternfly/patternfly/pull/4133))
  - Added string element with current/match support ([#4166](https://github.com/patternfly/patternfly/pull/4166))
- **Menu:** Updated content overflow ([#4157](https://github.com/patternfly/patternfly/pull/4157))
- **Notification drawer:** Updated demo notification text to reflect demo state ([#4117](https://github.com/patternfly/patternfly/pull/4117))
- **Overflow menu:** Added aria-expanded to examples ([#4137](https://github.com/patternfly/patternfly/pull/4137))
- **Page:** Increased sticky page section z-index ([#4128](https://github.com/patternfly/patternfly/pull/4128))
- **Table:**
  - Added draggable variant ([#4120](https://github.com/patternfly/patternfly/pull/4120))
  - Added background color to draggable ghost row ([#4159](https://github.com/patternfly/patternfly/pull/4159))
- **Toolbar:** Added sticky variation ([#4134](https://github.com/patternfly/patternfly/pull/4134))
- **Tree table:** Added actions support ([#4135](https://github.com/patternfly/patternfly/pull/4135))

### Other
- **Global:** Promoted beta components ([#4122](https://github.com/patternfly/patternfly/pull/4122))
- **Build:** Updated to use dart sass ([#4164](https://github.com/patternfly/patternfly/pull/4164))


## 2021.07 release notes (2021-06-04)
Packages released:
- [@patternfly/patternfly@v4.108.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.108.2)

### Components
- **Card:** Added variation to remove actions negative margin offset ([#4071](https://github.com/patternfly/patternfly/pull/4071))
- **Content:** Added support for visited link styling ([#4067](https://github.com/patternfly/patternfly/pull/4067))
- **Description list:** Removed two duplicate variables ([#4088](https://github.com/patternfly/patternfly/pull/4088))
- **Drawer:** Added panel min-width/height ([#4091](https://github.com/patternfly/patternfly/pull/4091))
- **Dropdown:** Added support for aria-disabled items ([#4072](https://github.com/patternfly/patternfly/pull/4072))
- **Helper text:** Added helper text component ([#4089](https://github.com/patternfly/patternfly/pull/4089))
- **Label:** Added editable label ([#4097](https://github.com/patternfly/patternfly/pull/4097))
- **Masthead:** Updated responsive behavior ([#4107](https://github.com/patternfly/patternfly/pull/4107))
- **Menu:**
  - Modified scroll behavior ([#4033](https://github.com/patternfly/patternfly/pull/4033))
  - Added z-index ([#4078](https://github.com/patternfly/patternfly/pull/4078))
  - Hid scroll on transition ([#4087](https://github.com/patternfly/patternfly/pull/4087))
  - Removed pf-m-drilldown transition, fixed content height transition ([#4090](https://github.com/patternfly/patternfly/pull/4090))
  - Disabled pointer events on disabled list item ([#4102](https://github.com/patternfly/patternfly/pull/4102))
- **Page:**
  - Added center aligned variation ([#4011](https://github.com/patternfly/patternfly/pull/4011))
- **Select:** Fixed specificity issue with toggle typeahead ([#4126](https://github.com/patternfly/patternfly/pull/4126))
- **Simple list:** Removed bold, removed blue color on hover/focus/active ([#4099](https://github.com/patternfly/patternfly/pull/4099))
- **Skeleton:** Improved animation performance ([#3967](https://github.com/patternfly/patternfly/pull/3967))
- **Tree view:** Added support for non-expandable top level nodes ([#4104](https://github.com/patternfly/patternfly/pull/4104))
- **Wizard:**
  - Unbolded current wizard nav item ([#4068](https://github.com/patternfly/patternfly/pull/4068))
  - Fixed expandable nav item toggle rotation/padding issue ([#4112](https://github.com/patternfly/patternfly/pull/4112))

### Other
- **Layouts:** Added examples using lists ([#4010](https://github.com/patternfly/patternfly/pull/4010))
- **Demos:** Updated user dropdown in page demos to use plain toggle ([#4070](https://github.com/patternfly/patternfly/pull/4070))
- **Icons:** Added panel-close and panel-open ([#4074](https://github.com/patternfly/patternfly/pull/4074))


## 2021.06 release notes (2021-05-14)
Packages released:
- [@patternfly/patternfly@v4.103.6](https://www.npmjs.com/package/@patternfly/patternfly/v/4.103.6)

### Components
- **Card:**
  - Put expandable right aligned toggle in regular DOM order ([#4045](https://github.com/patternfly/patternfly/pull/4045))
  - Updated right aligned expandable toggle spacing ([#4050](https://github.com/patternfly/patternfly/pull/4050))
- **Data list:** Fixed toggle icon bug with nested data lists ([#4041](https://github.com/patternfly/patternfly/pull/4041))
- **Divider:** Updated to not shrink in flex layouts ([#4016](https://github.com/patternfly/patternfly/pull/4016))
- **Drawer:** Made panel main/body elements fill height on mobile ([#4052](https://github.com/patternfly/patternfly/pull/4052))
- **Log viewer:**
  - Added log viewer ([#4029](https://github.com/patternfly/patternfly/pull/4029))
  - Removed hover, added scroller ([#4042](https://github.com/patternfly/patternfly/pull/4042))
- **Toggle group:**
  - Updated colors, borders, removed light, added compact ([#4054](https://github.com/patternfly/patternfly/pull/4054))
  - Moved borders to before pseudo element ([#4059](https://github.com/patternfly/patternfly/pull/4059))


## 2021.05 release notes (2021-04-20)
Packages released:
- [@patternfly/patternfly@v4.102.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.102.2)

### Components
- **Avatar:** Added dark and light variants ([#3990](https://github.com/patternfly/patternfly/pull/3990))
- **Button:** Reverted variations to regular property declarations ([#3997](https://github.com/patternfly/patternfly/pull/3997))
- **Card:**
  - Added empty state card back into demo ([#3961](https://github.com/patternfly/patternfly/pull/3961))
  - Fixed expandable toggle rotation ([#3981](https://github.com/patternfly/patternfly/pull/3981))
  - Added card demo images ([#3985](https://github.com/patternfly/patternfly/pull/3985))
  - Moved card view demo to demos, renamed card demo files ([#3989](https://github.com/patternfly/patternfly/pull/3989))
  - Updated empty state text in card view demo ([#4001](https://github.com/patternfly/patternfly/pull/4001))
- **Context selector:** Left aligned footer ([#3978](https://github.com/patternfly/patternfly/pull/3978))
- **Copy clipboard:** Renamed inline examples to inline compact ([#3998](https://github.com/patternfly/patternfly/pull/3998))
- **Form:** Updated element list for section title ([#3973](https://github.com/patternfly/patternfly/pull/3973))
- **Input group:** Fixed focus ring z-index issue ([#3991](https://github.com/patternfly/patternfly/pull/3991))
- **Menu:**
  - Added variation for top and left flyout menus ([#3977](https://github.com/patternfly/patternfly/pull/3977))
  - Added scrollable support ([#3999](https://github.com/patternfly/patternfly/pull/3999))
  - Added docs for load, loading, footer ([#4006](https://github.com/patternfly/patternfly/pull/4006))
- **Select:**
  - Added view more and loading support to select and menu ([#3968](https://github.com/patternfly/patternfly/pull/3968))
  - Updated typeahead text input height to match form control ([#3988](https://github.com/patternfly/patternfly/pull/3988))
  - Added warning and success states ([#4008](https://github.com/patternfly/patternfly/pull/4008))
- **Table:**
  - Added tree table responsiveness ([#3943](https://github.com/patternfly/patternfly/pull/3943))
  - Fixed tree text display ([#4000](https://github.com/patternfly/patternfly/pull/4000))
  - Fixed indendation for non-expandable tree table rows ([#4015](https://github.com/patternfly/patternfly/pull/4015))
- **Toolbar:**
  - Changed expanded content z-index to sm to match other menus ([#3986](https://github.com/patternfly/patternfly/pull/3986))
  - Added item width control ([#3994](https://github.com/patternfly/patternfly/pull/3994))

### Other
- **Workspace:** Fixed whitespace and code block ([#3956](https://github.com/patternfly/patternfly/pull/3956))
- **Stalebot:** Added closeComment to fix stalebot not running ([#3992](https://github.com/patternfly/patternfly/pull/3992))


## 2021.04 release notes (2021-03-30)
Packages released:
- [@patternfly/patternfly@v4.96.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.96.2)

### Components
- **Brand:** Added picture + srcset ([#3922](https://github.com/patternfly/patternfly/pull/3922))
- **Button:**
  - Added secondary/link danger variations ([#3932](https://github.com/patternfly/patternfly/pull/3932))
  - Reverted active state styling with new danger variations ([#3949](https://github.com/patternfly/patternfly/pull/3949))
- **Card:** Added card demos ([#3848](https://github.com/patternfly/patternfly/pull/3848))
- **Check/radio:** Updated body/description elements to be spans ([#3945](https://github.com/patternfly/patternfly/pull/3945))
- **Clipboard copy:** Added inline variant ([#3933](https://github.com/patternfly/patternfly/pull/3933))
- **Code block:** Added code block component ([#3937](https://github.com/patternfly/patternfly/pull/3937))
- **Dropdown:** Reverted badge menu to dropdown menu ([#3929](https://github.com/patternfly/patternfly/pull/3929))
- **Form:** Removed used of pf-m-expandable from field groups ([#3942](https://github.com/patternfly/patternfly/pull/3942))
- **Select:**
  - Added support for item count ([#3931](https://github.com/patternfly/patternfly/pull/3931))
  - Added invalid state ([#3940](https://github.com/patternfly/patternfly/pull/3940))
- **Tabs:** Added tabs demos ([#3914](https://github.com/patternfly/patternfly/pull/3914))
- **Wizard:** Added expandable subsections ([#3927](https://github.com/patternfly/patternfly/pull/3927))

### Other
- **Workspace:**
  - Added images for card demos ([#3925](https://github.com/patternfly/patternfly/pull/3925))
  - Updated example HBS spacing ([#3934](https://github.com/patternfly/patternfly/pull/3934))
  - Reverted to old html-formatter ([#3939](https://github.com/patternfly/patternfly/pull/3939))


## 2021.03 release notes (2021-03-09)
Packages released:
- [@patternfly/patternfly@v4.90.5](https://www.npmjs.com/package/@patternfly/patternfly/v/4.90.5)

### Components
- **Accordion:** Added display-lg, bordered, support multiple bodies ([#3888](https://github.com/patternfly/patternfly/pull/3888))
- **Breadcrumb:** Added docs, example for items as buttons ([#3901](https://github.com/patternfly/patternfly/pull/3901))
- **Check/radio:** Added support for custom body content ([#3884](https://github.com/patternfly/patternfly/pull/3884))
- **Context selector:**
  - Hid menu scroll unless there is overflow ([#3873](https://github.com/patternfly/patternfly/pull/3873))
  - Added suppport for items as links ([#3875](https://github.com/patternfly/patternfly/pull/3875))
- **Form control:** Added vars for textarea width/height for resizing ([#3883](https://github.com/patternfly/patternfly/pull/3883))
- **Login:** Added aria-hidden to hide/show password example fa icons ([#3877](https://github.com/patternfly/patternfly/pull/3877))
- **Options menu, dropdown:** Normalized plain toggle size ([#3878](https://github.com/patternfly/patternfly/pull/3878))
- **Page:** Fixed missing masthead demo screenshot ([#3917](https://github.com/patternfly/patternfly/pull/3917))
- **Search input:**
  - Added autocomplete ([#3892](https://github.com/patternfly/patternfly/pull/3892))
  - Fixed underline bug ([#3905](https://github.com/patternfly/patternfly/pull/3905))
- **Slider:** Added support for disabled ([#3879](https://github.com/patternfly/patternfly/pull/3879))
- **Split:** Added variation to allow children to wrap ([#3887](https://github.com/patternfly/patternfly/pull/3887))
- **Switch:** Adjusted space between switch and label ([#3882](https://github.com/patternfly/patternfly/pull/3882))
- **Table:**
  - Fixed treeview alignment ([#3890](https://github.com/patternfly/patternfly/pull/3890))
  - Fixed hoverable, selectable table ([#3893](https://github.com/patternfly/patternfly/pull/3893))


## 2021.02 release notes (2021-02-17)
Packages released:
- [@patternfly/patternfly@v4.87.3](https://www.npmjs.com/package/@patternfly/patternfly/v/4.87.3)

### Components
- **Calendar month:** Fixed var name ([#3841](https://github.com/patternfly/patternfly/pull/3841))
- **Description list:**
  - Added horizontal/vertical responsive mods ([#3815](https://github.com/patternfly/patternfly/pull/3815))
  - Added help text support ([#3816](https://github.com/patternfly/patternfly/pull/3816))
- **Drawer:**
  - Added light-200 variation for grey background ([#3819](https://github.com/patternfly/patternfly/pull/3819))
  - Disabled drawer transitions, pointer-events while resizing ([#3832](https://github.com/patternfly/patternfly/pull/3832))
  - Replace clamp in resizable drawer for browser support ([#3850](https://github.com/patternfly/patternfly/pull/3850))
  - Fixed bottom variant issues in safari ([#3860](https://github.com/patternfly/patternfly/pull/3860))
- **Jump links:** Added vertical jump links demos ([#3807](https://github.com/patternfly/patternfly/pull/3807))
- **Label:** Updated colors and add non-transparent border to work better on a gray background ([#3817](https://github.com/patternfly/patternfly/pull/3817))
- **Login:** Added support for hide/show password ([#3820](https://github.com/patternfly/patternfly/pull/3820))
- **Masthead:** Added masthead component ([#3716](https://github.com/patternfly/patternfly/pull/3716))
- **Menu toggle:** Added menu toggle component ([#3845](https://github.com/patternfly/patternfly/pull/3845))
- **Modal box:** Updated modal-box__header to not shrink ([#3826](https://github.com/patternfly/patternfly/pull/3826))
- **Radio, check:** Added standalone variation, cursor styles ([#3821](https://github.com/patternfly/patternfly/pull/3821))
- **Search input:** Fixed input hover bottom border ([#3843](https://github.com/patternfly/patternfly/pull/3843))
- **Slider:** Increased thumb target size([#3859](https://github.com/patternfly/patternfly/pull/3859))
- **Table:**
  - Added hoverable, selected rows ([#3835](https://github.com/patternfly/patternfly/pull/3835))
  - Added tree view support ([#3846](https://github.com/patternfly/patternfly/pull/3846))
- **Tabs:** Added responsive state for vertical tabs ([#3836](https://github.com/patternfly/patternfly/pull/3836))
- **Wizard:** Set in page wizard page section to shrink so footer/nav are sticky ([#3822](https://github.com/patternfly/patternfly/pull/3822))

### Other
- **Workspace:** Fixed missing outlines for utilities examples ([#3824](https://github.com/patternfly/patternfly/pull/3824))


## 2021.01 release notes (2021-01-26)
Packages released:
- [@patternfly/patternfly@v4.80.3](https://www.npmjs.com/package/@patternfly/patternfly/v/4.80.3)

### Components
- **Accordion:** Updated expanded content color to use correct global var ([#3723](https://github.com/patternfly/patternfly/pull/3723))
- **Alert group:** Updated examples and documentation ([#3757](https://github.com/patternfly/patternfly/pull/3757))
- **Card:**
  - Added demos for horizontal cards ([#3758](https://github.com/patternfly/patternfly/pull/3758))
  - Introduced large card variant ([#3793](https://github.com/patternfly/patternfly/pull/3793))
  - Added empty state card into the demo ([#3794](https://github.com/patternfly/patternfly/pull/3794))
- **Data list:** Updated alignment settings ([#3750](https://github.com/patternfly/patternfly/pull/3750))
- **Drawer:**
  - Fixed splitter scroll off when scroll the panel ([#3778](https://github.com/patternfly/patternfly/pull/3778))
  - Fixed inline bottom panel layout issues ([#3785](https://github.com/patternfly/patternfly/pull/3785))
  - Limited drawer resize to desktop breakpoint ([#3788](https://github.com/patternfly/patternfly/pull/3788))
- **Dropdown:** Added support for menu alignment at different breakpoints ([#3746](https://github.com/patternfly/patternfly/pull/3746))
- **Dropdown, breadcrumbs, menu:** Added badge to dropdown, badge dropdown to breadcrumbs, breadcrumbs to menu ([#3797](https://github.com/patternfly/patternfly/pull/3797))
- **Form:** Added demos, section title, stack control group mod ([#3767](https://github.com/patternfly/patternfly/pull/3767))
- **Form control:** Added placeholder variation for form select ([#3790](https://github.com/patternfly/patternfly/pull/3790))
- **Jump links:**
  - Updated focus styles to match hover ([#3786](https://github.com/patternfly/patternfly/pull/3786))
  - Added expandable variation ([#3802](https://github.com/patternfly/patternfly/pull/3802))
- **List:** Added bordered, image variants ([#3798](https://github.com/patternfly/patternfly/pull/3798))
- **Menu:** Updated __content height ([#3792](https://github.com/patternfly/patternfly/pull/3792))
- **Nav, tabs:** Added scroll-snapping ([#3754](https://github.com/patternfly/patternfly/pull/3754))
- **Number input:** Renamed touchspin to number input, use number type ([#3748](https://github.com/patternfly/patternfly/pull/3748))
- **Page:** Fixed page section responsive padding ([#3769](https://github.com/patternfly/patternfly/pull/3769))
- **Search input:** Added advanced search ([#3783](https://github.com/patternfly/patternfly/pull/3783))
- **Sidebar:** Added sidebar component ([#3801](https://github.com/patternfly/patternfly/pull/3801))
- **Spinner:** Added svg variation ([#3690](https://github.com/patternfly/patternfly/pull/3690))
- **Switch, radio, check:** Updated input height and grid ([#3749](https://github.com/patternfly/patternfly/pull/3749))
- **Table:** Fixed th alignment ([#3799](https://github.com/patternfly/patternfly/pull/3799))
- **Toggle group:** Added type=button to buttons ([#3760](https://github.com/patternfly/patternfly/pull/3760))
- **Toolbar:** Added toolbar back to demos ([#3753](https://github.com/patternfly/patternfly/pull/3753))
- **Wizard:** Added cancel button element to increase cancel spacer ([#3756](https://github.com/patternfly/patternfly/pull/3756))

### Other
- **Workspace:**
  - Updated the new component/layout/demo generator template to not escape --attribute ([#3739](https://github.com/patternfly/patternfly/pull/3739))
  - Cleaned up menu examples, code ([#3744](https://github.com/patternfly/patternfly/pull/3744))
  - Refactored dropdown example handlebars ([#3752](https://github.com/patternfly/patternfly/pull/3752))
- **Global CSS:**
  - Removed breakpoint map in pf-apply-breakpoint function ([#3761](https://github.com/patternfly/patternfly/pull/3761))
  - Added SVG vertical-align class for patternfly-react ([#3775](https://github.com/patternfly/patternfly/pull/3775))

## 2020.16 release notes (2020-12-08)
Packages released:
- [@patternfly/patternfly@v4.70.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.70.1)

### Components
- **Calendar month:**
  - Updated a11y per the react component ([#3696](https://github.com/patternfly/patternfly/pull/3696))
  - Updated in-range bg to stay within cell padding ([#3705](https://github.com/patternfly/patternfly/pull/3705))
- **Card:** Added support for dividers between sections ([#3485](https://github.com/patternfly/patternfly/pull/3485))
- **Drawer:** Kept content from shifting when panel resizes ([#3719](https://github.com/patternfly/patternfly/pull/3719))
- **Dual list selector:**
  - Fixed alignment, check selected style, examples ([#3730](https://github.com/patternfly/patternfly/pull/3730))
  - Fixed check item alignment issue ([#3734](https://github.com/patternfly/patternfly/pull/3734))
- **Form:** Added field groups ([#3654](https://github.com/patternfly/patternfly/pull/3654))
- **Jump links:** Kept sublists from inheriting current styles from parent ([#3707](https://github.com/patternfly/patternfly/pull/3707))
- **Page:** Moved nav specific demos into nav demos ([#3681](https://github.com/patternfly/patternfly/pull/3681))
- **Pagination:** Added modifers to switch between full and summary layouts ([#3684](https://github.com/patternfly/patternfly/pull/3684))
- **Dropdown:** Fixed plain toggle height inconsistency ([#3689](https://github.com/patternfly/patternfly/pull/3689))
- **Progress:** Enabled static width for measure value ([#3567](https://github.com/patternfly/patternfly/pull/3567))
- **Slider:** Added slider component ([#3711](https://github.com/patternfly/patternfly/pull/3711))
- **Table:**
  - Enabled borderless variation on all tables ([#3691](https://github.com/patternfly/patternfly/pull/3691))
  - Added var for sortable text, updated sortable favorite color ([#3732](https://github.com/patternfly/patternfly/pull/3732))
- **Tree view:**
  - Removed references to pf-m-expandable ([#3676](https://github.com/patternfly/patternfly/pull/3676))
  - Made toggle size/indentation same between core and react ([#3728](https://github.com/patternfly/patternfly/pull/3728))

### Layouts
- **Gallery:** Added support for custom item max-width ([#3703](https://github.com/patternfly/patternfly/pull/3703))

### Other
- **Stalebot:** Added pinned to exemptLabels ([#3685](https://github.com/patternfly/patternfly/pull/3685))
- **Global**: Added patternfly-base-no-reset.css ([#3736](https://github.com/patternfly/patternfly/pull/3736))


## 2020.15 release notes (2020-11-17)
Packages released:
- [@patternfly/patternfly@v4.65.5](https://www.npmjs.com/package/@patternfly/patternfly/v/4.65.5)

### Components
- **Action list:** Refactored css ([#3662](https://github.com/patternfly/patternfly/pull/3662))
- **Alert:** Added examples to support custom alert icon ([#3641](https://github.com/patternfly/patternfly/pull/3641))
- **Calendar month:**
  - Added calendar month component ([#3633](https://github.com/patternfly/patternfly/pull/3633))
  - Removed focus outline, fixed selected styling ([#3669](https://github.com/patternfly/patternfly/pull/3669))
  - Differentiated hover/focus styles ([#3671](https://github.com/patternfly/patternfly/pull/3671))
- **Description list:** Updated auto-fit var names ([#3613](https://github.com/patternfly/patternfly/pull/3613))
- **Drawer:**
  - Added resizable drawer, moved splitter ([#3659](https://github.com/patternfly/patternfly/pull/3659))
  - Updated demos, remove notification drawer demos ([#3621](https://github.com/patternfly/patternfly/pull/3621))
- **Dual list selector:** Add tree view feat ([#3656](https://github.com/patternfly/patternfly/pull/3656))
- **Form:** Added grid example, tidy hbs, change 2nd action to link ([#3422](https://github.com/patternfly/patternfly/pull/3422))
- **Jump links:** Added subsections ([#3632](https://github.com/patternfly/patternfly/pull/3632))
- **Notification drawer:** Updated demos so badge reflects drawer items ([#3622](https://github.com/patternfly/patternfly/pull/3622))
- **Popover:** Added variations for no-padding, auto-width ([#3646](https://github.com/patternfly/patternfly/pull/3646))
- **Tree view:**
  - Fixed indentation ([#3661](https://github.com/patternfly/patternfly/pull/3661))
  - Added toggle button for expandable with checkbox ([#3665](https://github.com/patternfly/patternfly/pull/3665))
- **Page:** Move nav specific page demos into nav demos ([#3681](https://github.com/patternfly/patternfly/pull/3681))

### Other
- **Docs:** Moved get started content to new developer resources section ([#3649](https://github.com/patternfly/patternfly/pull/3649))
- **Global vars:** Updated active-color--300 and 400 ([#3619](https://github.com/patternfly/patternfly/pull/3619))
- **Pficons:** Added new-process, not-started, resources-empty icons ([#3663](https://github.com/patternfly/patternfly/pull/3663))
- **Utilities:** Added text and background color utility classes ([#3439](https://github.com/patternfly/patternfly/pull/3439))


## 2020.14 release notes (2020-10-27)
Packages released:
- [@patternfly/patternfly@v4.59.1](https://www.npmjs.com/package/@patternfly/patternfly/v/4.59.1)

### Components
- **Action list:** Added action list component ([#3598](https://github.com/patternfly/patternfly/pull/3598))
- **Card:** Added expandable variation ([#3586](https://github.com/patternfly/patternfly/pull/3586))
- **Data list:**
  - Added variation to avoid shift when item is dragged over ([#3574](https://github.com/patternfly/patternfly/pull/3574))
- **Dual list selector:** Added dual list selector component ([#3605](https://github.com/patternfly/patternfly/pull/3605))
- **Expandable section:** Fixed nested component icon transform ([#3545](https://github.com/patternfly/patternfly/pull/3545))
- **Form control:**
  - Updated placeholder block so the color change applies ([#3579](https://github.com/patternfly/patternfly/pull/3579))
  - Updated select arrow to align with other menu toggles ([#3581](https://github.com/patternfly/patternfly/pull/3581))
- **Gallery:** Added width variable ([#3549](https://github.com/patternfly/patternfly/pull/3549))
- **Grid:** Updated order CSS to use mixin ([#3584](https://github.com/patternfly/patternfly/pull/3584))
- **Jump links:** Added jump links component ([#3596](https://github.com/patternfly/patternfly/pull/3596))
- **Menu:** Added drilldown menu ([#3438](https://github.com/patternfly/patternfly/pull/3438))
- **Modal:**
  - Right aligned help button, aligned with close button ([#3603](https://github.com/patternfly/patternfly/pull/3603))
  - Increased space between close button and what preceedes it ([#3588](https://github.com/patternfly/patternfly/pull/3588))
- **Page:** Made last child fill available space, not last-of-type ([#3609](https://github.com/patternfly/patternfly/pull/3609))
- **Pagination:** Updated sticky examples with content ([#3538](https://github.com/patternfly/patternfly/pull/3538))
- **Table:**
  - Removed compound expansion active border ([#3421](https://github.com/patternfly/patternfly/pull/3421))
  - Added examples with multiple expandable cells ([#3573](https://github.com/patternfly/patternfly/pull/3573))
  - Added favorites ([#3594](https://github.com/patternfly/patternfly/pull/3594))
- **Toggle group:** Updated button height to match other form elements ([#3576](https://github.com/patternfly/patternfly/pull/3576))
- **Toolbar:** Added support for expand all button ([#3601](https://github.com/patternfly/patternfly/pull/3601))
- **Touchspin:** Added touchspin component ([#3604](https://github.com/patternfly/patternfly/pull/3604))
- **Tree view:** Moved actions element out of node ([#3593](https://github.com/patternfly/patternfly/pull/3593))
- **Description list:** Added auto-fit varition ([#3553](https://github.com/patternfly/patternfly/pull/3553))

### Other
- **Build:** Stopped uploading artifacts ([#3542](https://github.com/patternfly/patternfly/pull/3542))
- **SCSS:**
  - Added quotes around sass vars to retain quotes when compiled ([#3582](https://github.com/patternfly/patternfly/pull/3582))
  - Added css var stack responsive mixin ([#3583](https://github.com/patternfly/patternfly/pull/3583))


## 2020.13 release notes (2020-10-06)
Packages released:
- [@patternfly/patternfly@v4.50.4](https://www.npmjs.com/package/@patternfly/patternfly/v/4.50.4)

### Components
- **Alert:**
  - Allowed long strings in description to wrap ([#3505](https://github.com/patternfly/patternfly/pull/3505))
  - Added inline variation demo ([#3517](https://github.com/patternfly/patternfly/pull/3517))
  - Fixed duplicate example IDs ([#3537](https://github.com/patternfly/patternfly/pull/3537))
- **Chip group:** Updated all examples to use `__main` ([#3530](https://github.com/patternfly/patternfly/pull/3530))
- **Context selector:**
  - Added actions footer ([#3494](https://github.com/patternfly/patternfly/pull/3494))
  - Fixed duplicate ID in examples ([#3533](https://github.com/patternfly/patternfly/pull/3533))
- **Data list:**
  - Added grid modifiers ([#3528](https://github.com/patternfly/patternfly/pull/3528))
  - Made data-list-grid an import in data-list ([#3540](https://github.com/patternfly/patternfly/pull/3540))
- **Date picker:**
  - Added date-picker, flatpickr styles ([#3482](https://github.com/patternfly/patternfly/pull/3482))
  - Added background to flatpickr-calendar wrapper ([#3510](https://github.com/patternfly/patternfly/pull/3510))
  - Kept text in month select from getting cut off ([#3521](https://github.com/patternfly/patternfly/pull/3521))
- **Empty state:** Added xs variation ([#3519](https://github.com/patternfly/patternfly/pull/3519))
- **Form control:** Fixed icon not showing in custom icon example ([#3524](https://github.com/patternfly/patternfly/pull/3524))
- **Form:** Added form section ([#3483](https://github.com/patternfly/patternfly/pull/3483))
- **Input group:** Fixed invalid state double border ([#3508](https://github.com/patternfly/patternfly/pull/3508))
- **Label group:** Positioned close button to the top right ([#3507](https://github.com/patternfly/patternfly/pull/3507))
- **Menu:** Updated example axe violations ([#3498](https://github.com/patternfly/patternfly/pull/3498))
- **Modal:**
  - Refactored the top aligned variation so it works better ([#3481](https://github.com/patternfly/patternfly/pull/3481))
  - Added support for title icon, modal alert states ([#3487](https://github.com/patternfly/patternfly/pull/3487))
  - Added help button ([#3495](https://github.com/patternfly/patternfly/pull/3495))
  - Added icon variant, renamed pf-m-error to pf-m-danger ([#3563](https://github.com/patternfly/patternfly/pull/3563))
- **Notification badge:** Added hover/focus styles for unread/attention ([#3488](https://github.com/patternfly/patternfly/pull/3488))
- **Page:** Added sticky sections, group, overflow scroll, shadows ([#3516](https://github.com/patternfly/patternfly/pull/3516))
- **Pagination:**
  - Added sticky position to bottom ([#3509](https://github.com/patternfly/patternfly/pull/3509))
  - Updated sticky examples with content ([#3538](https://github.com/patternfly/patternfly/pull/3538))
- **Select:** Fixed axe violations ([#3501](https://github.com/patternfly/patternfly/pull/3501))
- **Table:** Added support for single row radio select ([#3492](https://github.com/patternfly/patternfly/pull/3492))
- **Treeview:** Updated node actions to support dropdowns and buttons ([#3522](https://github.com/patternfly/patternfly/pull/3522))

### Layouts
- **Flex:** Added max-width to flex items ([#3479](https://github.com/patternfly/patternfly/pull/3479))
- **Flex & grid:** Added ordering to grid and flex layouts ([#3478](https://github.com/patternfly/patternfly/pull/3478))

### Other
- **Build:**
  - Updated size report ([#3534](https://github.com/patternfly/patternfly/pull/3534))
  - Reenable a11y ([#3506](https://github.com/patternfly/patternfly/pull/3506))
- **Readme:** Added steps to updated screenshots ([#3515](https://github.com/patternfly/patternfly/pull/3515))
- **Workspace:**
  - Added hmr for css ([#3499](https://github.com/patternfly/patternfly/pull/3499))
  - Fixed reloading bugs ([#3497](https://github.com/patternfly/patternfly/pull/3497))
  - Fixed broken links ([#3504](https://github.com/patternfly/patternfly/pull/3504))


## 2020.12 release notes (2020-09-17)
Packages released:
- [@patternfly/patternfly@v4.42.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.42.2)

### Components
- **Button:**
  - Added progress button ([#3382](https://github.com/patternfly/patternfly/pull/3382))
  - Added pf-m-link as an anchor example ([#3426](https://github.com/patternfly/patternfly/pull/3426))
  - Allowed inline link text to wrap and use with span element ([#3470](https://github.com/patternfly/patternfly/pull/3470))
- **Chipgroup:** Positioned removable button at the top ([#3445](https://github.com/patternfly/patternfly/pull/3445))
- **Codeeditor:** Added code editor ([#3457](https://github.com/patternfly/patternfly/pull/3457))
- **Datalist:**
  - Added draggable variation ([#3401](https://github.com/patternfly/patternfly/pull/3401))
  - Added text modifiers ([#3463](https://github.com/patternfly/patternfly/pull/3463))
- **Description list:** Updated term text element to be inline ([#3416](https://github.com/patternfly/patternfly/pull/3416))
- **Drawer:** Kept drawer panel from extending beyond drawer height ([#3460](https://github.com/patternfly/patternfly/pull/3460))
- **Dropdown, menu:** Made menu text work in dark theme ([#3446](https://github.com/patternfly/patternfly/pull/3446))
- **Form control:** Added ability to specify custom icon for text inputs ([#3453](https://github.com/patternfly/patternfly/pull/3453))
- **Label group:** Made left/right padding match in vertical layout ([#3466](https://github.com/patternfly/patternfly/pull/3466))
- **Modal:** Added align-top variation ([#3435](https://github.com/patternfly/patternfly/pull/3435))
- **Page:** Fixed safari bug w/ sections shrinking shorter than content ([#3441](https://github.com/patternfly/patternfly/pull/3441))
- **Pagination:** Update bottom example options menu to pf-m-top ([#3420](https://github.com/patternfly/patternfly/pull/3420))
- **Progress:** Added warning state ([#3423](https://github.com/patternfly/patternfly/pull/3423))
- **Progress:** Improved description/status text/wrapping ([#3436](https://github.com/patternfly/patternfly/pull/3436))
- **Select:** Added pf-m-focus support for favorite __menu-items ([#3431](https://github.com/patternfly/patternfly/pull/3431))
- **Table:**
  - Removed unused example code ([#3424](https://github.com/patternfly/patternfly/pull/3424))
  - Added help th support ([#3467](https://github.com/patternfly/patternfly/pull/3467))
- **Treeview:** Addressed follow-up issues from initial implementation ([#3410](https://github.com/patternfly/patternfly/pull/3410))

### Other
- **Docs:**
  - Updated form demo id to match component id ([#3428](https://github.com/patternfly/patternfly/pull/3428))
  - Added section for hover styles ([#3432](https://github.com/patternfly/patternfly/pull/3432))
- **Build:**
  - Bumped stylelint ([#3427](https://github.com/patternfly/patternfly/pull/3427))
  - Upgraded workspace to use new patternfly.org theme ([#3486](https://github.com/patternfly/patternfly/pull/3486))
  - Removed required "engines" section from package.json ([#3490](https://github.com/patternfly/patternfly/pull/3490))
- **Icons:**
  - Removed transforms from pficons, added namespaces ([#3398](https://github.com/patternfly/patternfly/pull/3398))
  - Updated pficon data to fix svg rendering issues ([#3469](https://github.com/patternfly/patternfly/pull/3469))


## 2020.11 release notes (2020-08-26)
Packages released:
- [@patternfly/patternfly@v4.35.2](https://www.npmjs.com/package/@patternfly/patternfly/v/4.35.2)

### Components
- **Button:** Added warning modifier to buttons ([#3349](https://github.com/patternfly/patternfly/pull/3349))
- **Drawer:**
  - Fixed drawer and notification drawer partial names ([#3370](https://github.com/patternfly/patternfly/pull/3370))
  - Added bottom panel variation ([#3408](https://github.com/patternfly/patternfly/pull/3408))
- **Dropdown:** Added border-radius to primary dropdown toggle ([#3377](https://github.com/patternfly/patternfly/pull/3377))
- **Form:** Hid negative margin overflow from `__actions` ([#3393](https://github.com/patternfly/patternfly/pull/3393))
- **Label group:** Added label group component, overflow label ([#3396](https://github.com/patternfly/patternfly/pull/3396))
- **Menu:** Added menu component ([#3397](https://github.com/patternfly/patternfly/pull/3397))
- **Notification drawer:** Added close button ([#3387](https://github.com/patternfly/patternfly/pull/3387))
- **Page:**
  - Added href to page-header-brand-link in examples ([#3383](https://github.com/patternfly/patternfly/pull/3383))
  - Added main section element examples ([#3390](https://github.com/patternfly/patternfly/pull/3390))
- **Skeleton:** Added width var, fixed safari bug w/ transparent ([#3385](https://github.com/patternfly/patternfly/pull/3385))
- **Splitter:** Added splitter component ([#3407](https://github.com/patternfly/patternfly/pull/3407))
- **Table:**
  - Added usage for using anchor to sort column headers ([#3386](https://github.com/patternfly/patternfly/pull/3386))
  - Updated loading demo text ([#3406](https://github.com/patternfly/patternfly/pull/3406))
- **Toggle group:** Added support for icon + text ([#3373](https://github.com/patternfly/patternfly/pull/3373))
- **Toolbar:**
  - Added insets ([#3403](https://github.com/patternfly/patternfly/pull/3403))
  - Added overflow menu to toolbar demos ([#3404](https://github.com/patternfly/patternfly/pull/3404))

### Other
- **Build:**
  - Updated stalebot ignored labels ([#3357](https://github.com/patternfly/patternfly/pull/3357))
  - Added github plugin ([#3359](https://github.com/patternfly/patternfly/pull/3359))
- **Workspace:**
  - Updated example titles/metadata ([#3363](https://github.com/patternfly/patternfly/pull/3363))
  - Updated examples for compatibility with new docs ([#3414](https://github.com/patternfly/patternfly/pull/3414))

## 2020.10 release notes (2020-08-17)
Packages released:
- [@patternfly/patternfly@v4.31.6](https://www.npmjs.com/package/@patternfly/patternfly/v/4.31.6)

### Components
- **Alert group:** Removed misformed metastring ([#3324](https://github.com/patternfly/patternfly/pull/3324))
- **Description list:**
  - Changed component to dl ([#3307](https://github.com/patternfly/patternfly/pull/3307))
  - Changed spacer width to 24px column gap ([#3327](https://github.com/patternfly/patternfly/pull/3327))
- **Drawer:**
  - Scoped `drawer__body > page__main` ([#3268](https://github.com/patternfly/patternfly/pull/3268))
  - Scoped drawer styles to improve behavior of nested drawers ([#3328](https://github.com/patternfly/patternfly/pull/3328))
- **Form control:** Added warning state, updated docs ([#3290](https://github.com/patternfly/patternfly/pull/3290))
- **Label:** Added support for overflow truncation ([#3339](https://github.com/patternfly/patternfly/pull/3339))
- **Notification badge:**
  - Fixed notification badge alignment ([#3284](https://github.com/patternfly/patternfly/pull/3284))
  - Reverted enhancements in #3231 ([#3294](https://github.com/patternfly/patternfly/pull/3294))
  - Allowed text to display on top of background ([#3365](https://github.com/patternfly/patternfly/pull/3365))
  - Removed unread state border ([#3389](https://github.com/patternfly/patternfly/pull/3389))
- **Notification drawer:**
  - Added notification drawer demo ([#3220](https://github.com/patternfly/patternfly/pull/3220))
  - Wrapped long item descriptions, group titles ([#3289](https://github.com/patternfly/patternfly/pull/3289))
  - Added default item variant ([#3338](https://github.com/patternfly/patternfly/pull/3338))
- **Page:**
  - Added ability to limit width of content in page section ([#3352](https://github.com/patternfly/patternfly/pull/3352))
  - Fixed notification badge state and header item conflict ([#3372](https://github.com/patternfly/patternfly/pull/3372))
- **Skeleton:** Added skeleton component ([#3353](https://github.com/patternfly/patternfly/pull/3353))
- **Tabs:** Added tab background modifiers ([#3286](https://github.com/patternfly/patternfly/pull/3286))
- **Table:** Applied no-padding modifiers to th ([#3323](https://github.com/patternfly/patternfly/pull/3323))
- **Tile:**
  - Removed support for imgs ([#3274](https://github.com/patternfly/patternfly/pull/3274))
  - Updated basic tile icon color to match stacked ([#3334](https://github.com/patternfly/patternfly/pull/3334))
- **Toggle group:** Added toggle group component ([#3355](https://github.com/patternfly/patternfly/pull/3355))
- **Treeview:** Added treeview component ([#3354](https://github.com/patternfly/patternfly/pull/3354))
- **Wizard:** Left aligned nav item element ([#3281](https://github.com/patternfly/patternfly/pull/3281))

### Other
- **Charts:** Adjusted chart tooltip padding ([#3347](https://github.com/patternfly/patternfly/pull/3347))
- **Build:**
  - Add compiled example html to dist ([#3301](https://github.com/patternfly/patternfly/pull/3301))
  - Added .github directory with stalebot configuration file ([#3336](https://github.com/patternfly/patternfly/pull/3336))
- **Icons:**
  - Fixed svg filename in generator config ([#3305](https://github.com/patternfly/patternfly/pull/3305))
  - Added attention-bell icon ([#3309](https://github.com/patternfly/patternfly/pull/3309))
  - Updated canvas size, positioning for attention-bell ([#3330](https://github.com/patternfly/patternfly/pull/3330))
  - Added pf-icon-bell ([#3351](https://github.com/patternfly/patternfly/pull/3351))
- **Global:**
  - Enabled flag to exclude root font-size unset ([#3275](https://github.com/patternfly/patternfly/pull/3275))
  - Updated success-200 to be a shade lighter ([#3316](https://github.com/patternfly/patternfly/pull/3316))
  - Positioned sr-only class to top/left 0 to avoid overflow ([#3319](https://github.com/patternfly/patternfly/pull/3319))
- **Workspace:**
  - Updated example titles to be h3 ([#3299](https://github.com/patternfly/patternfly/pull/3299))
  - Watched more sass for hot reload ([#3300](https://github.com/patternfly/patternfly/pull/3300))
  - Parse new h3 example titles ([#3302](https://github.com/patternfly/patternfly/pull/3302))
  - Fixed incorrect example metastring ([#3324](https://github.com/patternfly/patternfly/pull/3324))
- **Demo:**
  - Renamed master detail demo to primary-detail ([#3322](https://github.com/patternfly/patternfly/pull/3322))

## 2020.09 release notes (2020-07-16)
Packages released:
- [@patternfly/patternfly@v4.23.3](https://www.npmjs.com/package/@patternfly/patternfly/v/4.23.3)

### Components
- **Alert:** Fixed heading order ([#3236](https://github.com/patternfly/patternfly/pull/3236))
- **Breadcrumb:** Enabled always using a divider and hiding the first one ([#3202](https://github.com/patternfly/patternfly/pull/3202))
- **Button:** Added CTA variation ([#3214](https://github.com/patternfly/patternfly/pull/3214))
- **Chip:** Updated docs ([#3103](https://github.com/patternfly/patternfly/pull/3103))
- **Chip group:** Updated removable action icon ([#3249](https://github.com/patternfly/patternfly/pull/3249))
- **Chip & chip group:** Updated to use `ch` unit for chip and chip group label max-widths ([#3241](https://github.com/patternfly/patternfly/pull/3241))
- **Description list:** Added description list component ([#3243](https://github.com/patternfly/patternfly/pull/3243))
- **Form control:** Adjusted select arrow size ([#3207](https://github.com/patternfly/patternfly/pull/3207))
- **Hint:** Added hint component ([#3218](https://github.com/patternfly/patternfly/pull/3218))
- **Input group:** Updated example aria-label ([#3143](https://github.com/patternfly/patternfly/pull/3143))
- **Modal:** Added medium, adjusted maxwidths for better responsiveness ([#3217](https://github.com/patternfly/patternfly/pull/3217))
- **Nav:** Updated expandable nav to use `<button>` ([#3250](https://github.com/patternfly/patternfly/pull/3250))
- **Notification badge:**
  - Updated layout and design ([#3231](https://github.com/patternfly/patternfly/pull/3231))
  - Reverted enhancements in #3231 ([#3294](https://github.com/patternfly/patternfly/pull/3294))
- **Notification drawer:** Fixed duplicate ids ([#3237](https://github.com/patternfly/patternfly/pull/3237))
- **Search input:** Added search input component ([#3264](https://github.com/patternfly/patternfly/pull/3264))
- **Select:**
  - Added support for checkbox select with description ([#3224](https://github.com/patternfly/patternfly/pull/3224))
  - Added favorites ([#3238](https://github.com/patternfly/patternfly/pull/3238))
- **Table:** Added aria-label to help button ([#3266](https://github.com/patternfly/patternfly/pull/3266))
- **Tile:** Added tile component ([#3229](https://github.com/patternfly/patternfly/pull/3229))
- **Toolbar:** Removed beta flag from docs ([#3234](https://github.com/patternfly/patternfly/pull/3234))
- **Wizard:** Added support for buttons as nav items ([#3216](https://github.com/patternfly/patternfly/pull/3216))

### Other
- **Workspace:**
  - Changed title to filepath ([#3232](https://github.com/patternfly/patternfly/pull/3232))
  - Enabled theme `wrapperTag` to avoid having two `<main>`s on fullscreen pages ([#3233](https://github.com/patternfly/patternfly/pull/3233))
- **Build:**
  - Added new component/layout/demo generator script ([#3235](https://github.com/patternfly/patternfly/pull/3235))
  - Updated size report to use commander, fixed code ([#3242](https://github.com/patternfly/patternfly/pull/3242))
  - Disabled size report running against master ([#3244](https://github.com/patternfly/patternfly/pull/3244))

## 2020.08 release notes (2020-06-24)
Packages released:
- [@patternfly/patternfly@v4.16.7](https://www.npmjs.com/package/@patternfly/patternfly/v/4.16.7)

### Components
- **Alert group:** Updated basic example to inline, removed actions ([#3142](https://github.com/patternfly/patternfly/pull/3142))
- **Banner:** Added banner component ([#2814](https://github.com/patternfly/patternfly/pull/2814))
- **Button:**
  - Added small variation ([#3132](https://github.com/patternfly/patternfly/pull/3132))
  - Added pf-m-aria-disabled state ([#3166](https://github.com/patternfly/patternfly/pull/3166))
  - Made inline link inherit font-size, updated alert actions ([#3192](https://github.com/patternfly/patternfly/pull/3192))
- **Divider:** Added xs spacer to inset map ([#3191](https://github.com/patternfly/patternfly/pull/3191))
- **Drawer:**
  - Kept panel content from displaying on top of divider/border ([#3180](https://github.com/patternfly/patternfly/pull/3180))
  - Added tab focus fix ([#3184](https://github.com/patternfly/patternfly/pull/3184))
- **Dropdown/select:** Added description element ([#3130](https://github.com/patternfly/patternfly/pull/3130))
- **Label:** Added purple-50 global color, updated green/purple fill colors ([#3138](https://github.com/patternfly/patternfly/pull/3138))
- **Modal:** Allowed wizard height to shrink when used in modal ([#3176](https://github.com/patternfly/patternfly/pull/3176))
- **Notification drawer:** Fixed last item's color line position ([#3149](https://github.com/patternfly/patternfly/pull/3149))
- **Notification drawer/alert:** Improved title wrapping ([#3145](https://github.com/patternfly/patternfly/pull/3145))
- **Table:**
  - Added sticky-header variation ([#3093](https://github.com/patternfly/patternfly/pull/3093))
  - Added support for column header help ([#3189](https://github.com/patternfly/patternfly/pull/3189))
  - Updated sticky header border var so border is visible ([#3197](https://github.com/patternfly/patternfly/pull/3197))
- **Tabs:** Reduced secondary tab font-size ([#3135](https://github.com/patternfly/patternfly/pull/3135))

### Other
- **Global:** Added RedHatDisplay back as --FontFamily--heading--sans-serif ([#3188](https://github.com/patternfly/patternfly/pull/3188))
- **Build:**
  - Enabled publish to prerelease tag ([#3162](https://github.com/patternfly/patternfly/pull/3162))
  - Upgraded patternfly-a11y ([#3178](https://github.com/patternfly/patternfly/pull/3178))
- **Docs:**
  - Copied UPGRADE-GUIDE.md ([#3158](https://github.com/patternfly/patternfly/pull/3158))
  - Updated upgrade guide RedHatDisplay text ([#3195](https://github.com/patternfly/patternfly/pull/3195))
- **Workspace:** Updated gatsby version, removed footer ([#3155](https://github.com/patternfly/patternfly/pull/3155))

## 2020.07 release notes (2020-06-05)
Packages released:
- [@patternfly/patternfly@v4.10.31](https://www.npmjs.com/package/@patternfly/patternfly/v/4.10.31)

This is our major release. Check out our [upgrade guide](/developer-resources/upgrade-guide/html) for a list of breaking changes!

## 2020.06 release notes (2020-05-12)
Packages released:
- [@patternfly/patternfly@v2.71.6](https://www.npmjs.com/package/@patternfly/patternfly/v/2.71.6)

### Other
  - **Dev lite:** Removed examples and components on delete ([#2984](https://github.com/patternfly/patternfly/pull/2984))

## 2020.05 release notes (2020-04-21)
Packages released:
- [@patternfly/patternfly@v2.71.5](https://www.npmjs.com/package/@patternfly/patternfly/v/2.71.5)

### Other
- **Docs:**
  - Updated steps to be used for dev env ([#2952](https://github.com/patternfly/patternfly/pull/2952))
  - Added missing code of conduct ([#2945](https://github.com/patternfly/patternfly/pull/2945))
  - Fixed release notes formatting ([#2909](https://github.com/patternfly/patternfly/pull/2909))
  - Added missing step for building dev env ([#2957](https://github.com/patternfly/patternfly/pull/2957))
- **Build:**
  - Published demos to NPM ([#2908](https://github.com/patternfly/patternfly/pull/2908))

## 2020.04 release notes (2020-03-31)
Packages released:
- [@patternfly/patternfly@v2.71.3](https://www.npmjs.com/package/@patternfly/patternfly/v/2.71.3)

### Components
- **File upload:** Removed message container, added form to error example ([#2807](https://github.com/patternfly/patternfly/pull/2807))
- **Table:** Updated text in column management demo modal ([#2875](https://github.com/patternfly/patternfly/pull/2875))

### Other
- **Build:**
  - Fixed a11y upload report ([#2790](https://github.com/patternfly/patternfly/pull/2790))
  - Replaced 288 occurences of patternfly-next with patternfly ([#2880](https://github.com/patternfly/patternfly/pull/2880))
  - Publish docs to NPM ([#2839](https://github.com/patternfly/patternfly/pull/2839))
  - Added example CSS to dist ([#2840](https://github.com/patternfly/patternfly/pull/2840))
  - Parsed hbs files for PatternFly VS Code extension ([#2865](https://github.com/patternfly/patternfly/pull/2865))
- **Workspace:**
  - Scoped linting example CSS ([#2841](https://github.com/patternfly/patternfly/pull/2841))

## 2020.03 release notes (2020-03-10)
Packages released:
- [@patternfly/patternfly@v2.68.3](https://www.npmjs.com/package/@patternfly/patternfly/v/2.68.3)

### Components
- **Background image:** Removed empty width attribute ([#2739](https://github.com/patternfly/patternfly/pull/2739))
- **Divider:** Added vertical divider ([#2734](https://github.com/patternfly/patternfly/pull/2734))
- **File upload:**
  - Updated element names and general styles ([#2731](https://github.com/patternfly/patternfly/pull/2731))
  - Updated drag hover border ([#2776](https://github.com/patternfly/patternfly/pull/2776))
- **Login:** Centered login on larger viewports ([#2754](https://github.com/patternfly/patternfly/pull/2754))
- **Master detail:** Added master detail demo ([#2742](https://github.com/patternfly/patternfly/pull/2742))
- **Nav:** Adjusted divider positioning and color declarations([#2788](https://github.com/patternfly/patternfly/pull/2788))
- **Notification drawer:** Updated demo dropdowns so they dont overflow parent ([#2767](https://github.com/patternfly/patternfly/pull/2767))
- **Table:** Added table column management demo ([#2756](https://github.com/patternfly/patternfly/pull/2756))

### Other
- **Build:**
  - Updated gatsby theme ([#2737](https://github.com/patternfly/patternfly/pull/2737))
  - Added a comment to test v4 release ([#2764](https://github.com/patternfly/patternfly/pull/2764))
  - Removed unused file to test build ([#2771](https://github.com/patternfly/patternfly/pull/2771))
  - Added breaking change lint ([#2772](https://github.com/patternfly/patternfly/pull/2772))
  - Fixed upload for master ([#2780](https://github.com/patternfly/patternfly/pull/2780))
  - Testing a minor version bump ([#2784](https://github.com/patternfly/patternfly/pull/2784))
- **Workspace:**
  - Added class to body of fullscreen examples ([#2735](https://github.com/patternfly/patternfly/pull/2735))
  - Added example titles to index page ([#2741](https://github.com/patternfly/patternfly/pull/2741))
  - Updated example ids / classes ([#2749](https://github.com/patternfly/patternfly/pull/2749))
  - Added class to index, update styles ([#2750](https://github.com/patternfly/patternfly/pull/2750))
  - Scoped flex layout workspace styles to layout examples ([#2787](https://github.com/patternfly/patternfly/pull/2787))

## 2020.02 release notes (2020-02-18)
Packages released:
- [@patternfly/patternfly@v2.65.2](https://www.npmjs.com/package/@patternfly/patternfly/v/2.65.2)

### Components
- **Data list:**
  - Added compact version of data list ([#2686](https://github.com/patternfly/patternfly/pull/2686))
  - Enabled hidden/visible feature to update display ([#2691](https://github.com/patternfly/patternfly/pull/2691))
- **Data toolbar:** Updated input type to search ([#2648](https://github.com/patternfly/patternfly/pull/2648))
- **File upload:** Added file upload component ([#2681](https://github.com/patternfly/patternfly/pull/2681))
- **Inline edit:** Fixed firefox bug causes alignment issue ([#2706](https://github.com/patternfly/patternfly/pull/2706))
- **Master detail:**
  - Added master detail demo ([#2645](https://github.com/patternfly/patternfly/pull/2645))
  - Reverted master detail demo ([#2721](https://github.com/patternfly/patternfly/pull/2721))
- **Modal box:** Added modal description element ([#2646](https://github.com/patternfly/patternfly/pull/2646))
- **Nav:** Updated group title spacing so it changes responsively ([#2641](https://github.com/patternfly/patternfly/pull/2641))
- **Pagination:** Allowed updating input width with CSS vars ([#2664](https://github.com/patternfly/patternfly/pull/2664))
- **Popover:** Matched min/max-width so popover has consistent width ([#2660](https://github.com/patternfly/patternfly/pull/2660))
- **Select:**
  - Added filter and search input ([#2693](https://github.com/patternfly/patternfly/pull/2693))
  - Made badge optional in checkbox select toggle ([#2642](https://github.com/patternfly/patternfly/pull/2642))
- **Simple list:** Added tabindex, button type, removed hover underline ([#2679](https://github.com/patternfly/patternfly/pull/2679))
- **Switch:** Moved nested element rules to the root ([#2610](https://github.com/patternfly/patternfly/pull/2610))
- **Table:** Allowed sortable table headers to wrap ([#2668](https://github.com/patternfly/patternfly/pull/2668))
- **Title:** Enabled long strings to wrap ([#2662](https://github.com/patternfly/patternfly/pull/2662))
- **Toolbar:** Added toolbar layout CSS ([#2689](https://github.com/patternfly/patternfly/pull/2689))

### Other
- **Build:**
  - Added proper size report check ([#2638](https://github.com/patternfly/patternfly/pull/2638))
  - Wrote nicer ie11 error message ([#2640](https://github.com/patternfly/patternfly/pull/2640))
  - Created lightweight workspace ([#2665](https://github.com/patternfly/patternfly/pull/2665))
  - Added basic WS styling ([#2675](https://github.com/patternfly/patternfly/pull/2675))
  - Added wrapper to examples to fix example specific css ([#2678](https://github.com/patternfly/patternfly/pull/2678))
  - Phrased non production components as beta ([#2680](https://github.com/patternfly/patternfly/pull/2680))
  - Updated _all.scss with missing components ([#2684](https://github.com/patternfly/patternfly/pull/2684))
  - Removed unused directory ([#2685](https://github.com/patternfly/patternfly/pull/2685))
  - Added files/dirs to cleanup ([#2699](https://github.com/patternfly/patternfly/pull/2699))

## 2020.01 release notes (2020-01-28)
Packages released:
- [@patternfly/patternfly@v2.56.3](https://www.npmjs.com/package/@patternfly/patternfly/v/2.56.3)

### Components
- **Button:** Added ability to have icon on left or ride side of text ([#2548](https://github.com/patternfly/patternfly/pull/2548))
- **Card:** Added __head-main elements to contain images, icons, etc ([#2578](https://github.com/patternfly/patternfly/pull/2578))
- **Chip group:** Fixed chip label overflow ellipsis ([#2552](https://github.com/patternfly/patternfly/pull/2552))
- **Clipboard copy:** Assigned type to button to prevent form submit ([#2561](https://github.com/patternfly/patternfly/pull/2561))
- **Empty state:** Added xl variation ([#2545](https://github.com/patternfly/patternfly/pull/2545))
- **Inline edit:** Added inline-edit component ([#2446](https://github.com/patternfly/patternfly/pull/2446))
- **Input:** Added new example ([#2563](https://github.com/patternfly/patternfly/pull/2563))
- **Login:** Prevented login container from shrinking based on content ([#2604](https://github.com/patternfly/patternfly/pull/2604))
- **Master detail:** Added master-detail layout updates to drawer ([#2520](https://github.com/patternfly/patternfly/pull/2520))
- **Notification drawer:** Fixed flex shorthand bug resulting in 0 height ([#2571](https://github.com/patternfly/patternfly/pull/2571))
- **Page:** Added selected state to header icons, updated drawer demo ([#2541](https://github.com/patternfly/patternfly/pull/2541))
- **Radio and check:** Added optional description ([#2579](https://github.com/patternfly/patternfly/pull/2579))
- **Simple list:** Introduced simple list component ([#2573](https://github.com/patternfly/patternfly/pull/2573))
- **Table:**
  - Fixed missing expanded content border on mobile ([#2553](https://github.com/patternfly/patternfly/pull/2553))
  - Fixed bug with mobile inheritable grid-column property ([#2558](https://github.com/patternfly/patternfly/pull/2558))
  - Updated font-size for checks ([#2577](https://github.com/patternfly/patternfly/pull/2577))
  - Fixed accessibility issues on mobile ([#2582](https://github.com/patternfly/patternfly/pull/2582))
- **Wizard:** Bolded current sub-step link ([#2542](https://github.com/patternfly/patternfly/pull/2542))

### Other
- **Build:**
  - Removed "main" property from package.json ([#2549](https://github.com/patternfly/patternfly/pull/2549))
  - Promoted data-toolbar, divider, overflowmenu and spinner ([#2576](https://github.com/patternfly/patternfly/pull/2576))
  - Turned a11y checker back on ([#2585](https://github.com/patternfly/patternfly/pull/2585))
  - Enabled linting css ([#2586](https://github.com/patternfly/patternfly/pull/2586))
  - Enabled linting css size ([#2587](https://github.com/patternfly/patternfly/pull/2587))
  - Updated gatsby theme verion ([#2605](https://github.com/patternfly/patternfly/pull/2605))
  - Made data-toolbar experimental ([#2611](https://github.com/patternfly/patternfly/pull/2611))
- **Docs:** Fixed misspelling of "Overview" in multiple places ([#2566](https://github.com/patternfly/patternfly/pull/2566))
- **Global vars:** Added CSS vars for color palette SCSS vars ([#2551](https://github.com/patternfly/patternfly/pull/2551))

## 2019.11 release notes (2019-12-18)
Packages released:
- [@patternfly/patternfly@v2.46.1](https://www.npmjs.com/package/@patternfly/patternfly/v/2.46.1)

### Components
- **App launcher:**
  - Removed unused vars ([#2459](https://github.com/patternfly/patternfly/pull/2459))
  - Fixed spacing between favorites search and next item ([#2515](https://github.com/patternfly/patternfly/pull/2515))
- **Card:** Added selectable card ([#2497](https://github.com/patternfly/patternfly/pull/2497))
- **Charts:** Updated threshold properties ([#2486](https://github.com/patternfly/patternfly/pull/2486))
- **Data list:**
  - Added selectable/hoverable row variations ([#2491](https://github.com/patternfly/patternfly/pull/2491))
  - Split out selected row state vars ([#2502](https://github.com/patternfly/patternfly/pull/2502))
- **Data toolbar:** Added content wrapper, updated margins ([#2460](https://github.com/patternfly/patternfly/pull/2460))
- **Empty state:** Moved basic example ([#2499](https://github.com/patternfly/patternfly/pull/2499))
- **Global vars:**
  - Updated pf-color-black-200 and pf-color-black-600 ([#2477](https://github.com/patternfly/patternfly/pull/2477))
  - Updated success-color--100, success-color--200, green-500 ([#2480](https://github.com/patternfly/patternfly/pull/2480))
  - Reverted global color changes from #2477 and #2480 ([#2505](https://github.com/patternfly/patternfly/pull/2505))
- **Nav:**
  - Removed bold for tertiary link current, active, focus ([#2487](https://github.com/patternfly/patternfly/pull/2487))
  - Updated horizontal overflow arrow colors ([#2510](https://github.com/patternfly/patternfly/pull/2510))
- **Notification drawer:** Added notification drawer component ([#2511](https://github.com/patternfly/patternfly/pull/2511))
- **Options menu:**
  - Added support for groups and titles ([#2403](https://github.com/patternfly/patternfly/pull/2403))
  - Fixed spacing if separator is last item in group ([#2500](https://github.com/patternfly/patternfly/pull/2500))

### Other
- **Build:**
  - Enabled patternfly-a11y ([#2453](https://github.com/patternfly/patternfly/pull/2453))
  - Upgraded gatsby and org theme ([#2496](https://github.com/patternfly/patternfly/pull/2496))

## 2019.10 release notes (2019-11-25)
Packages released:
- [@patternfly/patternfly@v2.43.1](https://www.npmjs.com/package/@patternfly/patternfly/v/2.43.1)

### Components
- **App launcher:** Added support for search input, favorites ([#2428](https://github.com/patternfly/patternfly/pull/2428))
- **Data toolbar:**
  - Moved and wrapped chips ([#2397](https://github.com/patternfly/patternfly/pull/2397))
  - Reverted changes from refactor-data-toolbar branch ([#2434](https://github.com/patternfly/patternfly/pull/2434))
  - Unreverted changes from refactor-data-toolbar branch ([#2440](https://github.com/patternfly/patternfly/pull/2440))
  - Updated chip-group structure ([#2445](https://github.com/patternfly/patternfly/pull/2445))
- **Dropdown:**
  - Added regular action support in split button dropdown ([#2418](https://github.com/patternfly/patternfly/pull/2418))
  - Fixed bottom border for split button w/ action expanded ([#2449](https://github.com/patternfly/patternfly/pull/2449))
  - Added support for menu item icons ([#2451](https://github.com/patternfly/patternfly/pull/2451))
- **Page:** Fixed hamburger alignment on mobile ([#2402](https://github.com/patternfly/patternfly/pull/2402))
- **Select:** Removed use of form element in toggle and menu ([#2430](https://github.com/patternfly/patternfly/pull/2430))

### Other
- **Build:**
  - Bumped gatsby-theme-patternfly-org ([#2377](https://github.com/patternfly/patternfly/pull/2377))
  - Moved build back to separate component build for correct dist ([#2421](https://github.com/patternfly/patternfly/pull/2421))
  - Added /g to regex ([#2423](https://github.com/patternfly/patternfly/pull/2423))
  - Used surge to host pf4.patternfly.org ([#2429](https://github.com/patternfly/patternfly/pull/2429))
  - Updated to always auto-import patternfly-utilities.sass ([#2433](https://github.com/patternfly/patternfly/pull/2433))
- **Workspace:** Updated component titles to be sentence case ([#2401](https://github.com/patternfly/patternfly/pull/2401))

## 2019.09 release notes (2019-11-01)
Packages released:
- [@patternfly/patternfly@v2.40.6](https://www.npmjs.com/package/@patternfly/patternfly/v/2.40.6)

### Components
- **Charts:**
  - Added strokeDasharray variable for ChartThreshold ([#2314](https://github.com/patternfly/patternfly/pull/2314))
  - Added size variables for scatter chart ([#2340](https://github.com/patternfly/patternfly/pull/2340))
- **Input group:** Unnested text class ([#2318](https://github.com/patternfly/patternfly/pull/2318))
- **Alert:** Fixed inline alert examples ([#2354](https://github.com/patternfly/patternfly/pull/2354))
- **Nav:**
  - Updated nav separator color in dark theme ([#2317](https://github.com/patternfly/patternfly/pull/2317))
  - Updated examples so simple-list is only used in expandable nav ([#2387](https://github.com/patternfly/patternfly/pull/2387))
- **Form control:**
  - Added horizontal and vertical resize variants for `<textarea>` ([#2331](https://github.com/patternfly/patternfly/pull/2331))
  - Updated horizontal and vertical resize variant descriptions ([#2386](https://github.com/patternfly/patternfly/pull/2386))
- **Select:**
  - Added plain modifier to button ([#2364](https://github.com/patternfly/patternfly/pull/2364))
  - Added example of empty menu with a div ([#2337](https://github.com/patternfly/patternfly/pull/2337))
- **Data toolbar:**
  - Refactored examples to match react implementation ([#2342](https://github.com/patternfly/patternfly/pull/2342))
  - Added attribute-value filter toolbar demo ([#2287](https://github.com/patternfly/patternfly/pull/2287))
- **Options menu:** Made class work for svg ([#2341](https://github.com/patternfly/patternfly/pull/2341))
- **Popover:** Reduced space below title ([#2381](https://github.com/patternfly/patternfly/pull/2381))
- **Overflow menu:** Updated examples to match react integration ([#2328](https://github.com/patternfly/patternfly/pull/2328))
- **Form:** Added success form modifier ([#2338](https://github.com/patternfly/patternfly/pull/2338))
- **Chip group:** Added closable chip-group ([#2334](https://github.com/patternfly/patternfly/pull/2334))
- **Accordion:** Added no-box-shadow variation, refactored expanded border ([#2385](https://github.com/patternfly/patternfly/pull/2385))
- **Dropdown, options menu, app launcher:** Fixed menu group and separator spacing ([#2384](https://github.com/patternfly/patternfly/pull/2384))

### Other
- **Font:** Added monospace stack for opt in redhat font ([#2382](https://github.com/patternfly/patternfly/pull/2382))
- **Build:**
  - Hot-reload styles, fixed trailing slashes ([#2349](https://github.com/patternfly/patternfly/pull/2349))
  - Fixed duplicated placeholders and linting ([#2360](https://github.com/patternfly/patternfly/pull/2360))
  - Properly copy source SASS files to dist ([#2367](https://github.com/patternfly/patternfly/pull/2367))
  - Use cssnano for minification ([#2368](https://github.com/patternfly/patternfly/pull/2368))
  - Refactor/mdx followup ([#2369](https://github.com/patternfly/patternfly/pull/2369))
- **Docs:**
  - Use gatsby-theme-patternfly-org ([#2242](https://github.com/patternfly/patternfly/pull/2242))
  - Fixed component titles for Navigation and Application Launcher ([#2356](https://github.com/patternfly/patternfly/pull/2356))
  - Added HTML formatting ([#2363](https://github.com/patternfly/patternfly/pull/2363))
  - Bump gatsby-theme-patternfly-org ([#2372](https://github.com/patternfly/patternfly/pull/2372))

## 2019.08 release notes (2019-10-01)
Packages released:
- [@patternfly/patternfly@v2.33.5](https://www.npmjs.com/package/@patternfly/patternfly/v/2.33.5)

### Components
- **Button:** Add control modifier ([#2005](https://github.com/patternfly/patternfly/pull/2005))
- **Charts:**
  - Adjust padding for pie and donut charts ([#2247](https://github.com/patternfly/patternfly/pull/2247))
  - Set mix-blend-mode for better color contrast ([#2239](https://github.com/patternfly/patternfly/pull/2239))
  - Update area chart opacity ([#2233](https://github.com/patternfly/patternfly/pull/2233))
- **Chip group:** Add overflow to group ([#2278](https://github.com/patternfly/patternfly/pull/2278))
- **Copy to clipboard:** Wrapped code in pre which allows the code to preserve line-breaks and spaces and also applies styling such as the monospace font. ([#2260](https://github.com/patternfly/patternfly/pull/2260))
- **Data list:** Make data list borders consistent with table ([#2289](https://github.com/patternfly/patternfly/pull/2289))
- **Data toolbar:**
  - Updated clear button mods ([#2248](https://github.com/patternfly/patternfly/pull/2248))
  - Added in examples for expandable ([#2273](https://github.com/patternfly/patternfly/pull/2273))
- **Flex layout:** Wrapped flex items, update css ([#2214](https://github.com/patternfly/patternfly/pull/2214))
- **Navigation:** Added styling updates to dark nav ([#2283](https://github.com/patternfly/patternfly/pull/2283))
- **Page:** Added ability to keep sidebar collapsed between sizes ([#2264](https://github.com/patternfly/patternfly/pull/2264))
- **Pagination:** Added compact variation ([#2275](https://github.com/patternfly/patternfly/pull/2275))
- **Radio:** Updated the radio component example to single components ([#2231](https://github.com/patternfly/patternfly/pull/2231))
- **Select:**
  - Added typeahead form wrapper, updated css ([#2255](https://github.com/patternfly/patternfly/pull/2255))
  - Add top expanded example ([#2026](https://github.com/patternfly/patternfly/pull/2026))
- **Table:**
  - Fixed a11y issues in org documentation/html/table ([#2282](https://github.com/patternfly/patternfly/pull/2282))
  - Added empty and loading state table demos ([#2254](https://github.com/patternfly/patternfly/pull/2254))
- **Tooltip:** Added text align left modifier ([#2284](https://github.com/patternfly/patternfly/pull/2284))
- **Wizard:**
  - Re-enabled wizard modal demos ([#2259](https://github.com/patternfly/patternfly/pull/2259))
  - Updated in page wizard demos to use dark sidebar/nav ([#2296](https://github.com/patternfly/patternfly/pull/2296))

### Other
- **Shield:** Added missing components, missing var to brand docs js ([#2263](https://github.com/patternfly/patternfly/pull/2263))

## 2019.07 release notes (2019-09-10)
Packages released:
- [@patternfly/patternfly@v2.31.6](https://www.npmjs.com/package/@patternfly/patternfly/v/2.31.6)

### Components
- **Charts:**
  - Refactor bullet chart vars. [#2159](https://github.com/patternfly/patternfly/pull/2159)
  - Adjust chart axis label padding. [#2181](https://github.com/patternfly/patternfly/pull/2181)
- **Select:** Add support for custom toggle icon. [#2154](https://github.com/patternfly/patternfly/pull/2154)
- **Data Toolbar:**
  - Add data toolbar component. [#2119](https://github.com/patternfly/patternfly/pull/2119)
  - Refactor spacer SCSS. [#2189](https://github.com/patternfly/patternfly/pull/2189)
  - Fix group formatting in expandable content. [#2229](https://github.com/patternfly/patternfly/pull/2229)
- **Modal:** Add variation for left-aligned footer buttons. [#2182](https://github.com/patternfly/patternfly/pull/2182)
- **Wizard:** Add in-page variation. [#2186](https://github.com/patternfly/patternfly/pull/2186)
- **Dropdown:** Add variation for primary toggle. [#2210](https://github.com/patternfly/patternfly/pull/2210)
- **Spinner:** Add spinner component. [#2142](https://github.com/patternfly/patternfly/pull/2142)
- **Dropdown:**
  - Add toggle text to split-button variation. [#2212](https://github.com/patternfly/patternfly/pull/2212)
  - Fix split-button toggle text spacing. [#2224](https://github.com/patternfly/patternfly/pull/2224)
- **Overflow Menu:** Add overflow menu component. [#2126](https://github.com/patternfly/patternfly/pull/2126)
- **Nav & Page sidebar:**
  - Add variation for dark theme. [#2197](https://github.com/patternfly/patternfly/pull/2197)
  - Fix nav separator background in dark theme. [#2227](https://github.com/patternfly/patternfly/pull/2227)
- **Form:** Update horizontal form label alignment. [#2200](https://github.com/patternfly/patternfly/pull/2200)

### Other
- **A11y:** Fix accessibility issues in basic forms demo. [#2086](https://github.com/patternfly/patternfly/pull/2086)
- **Dependencies:**
  - Update development dependencies. ([#2124](https://github.com/patternfly/patternfly/pull/2124))
  - Update Gulp and bump dependencies. [#2201](https://github.com/patternfly/patternfly/pull/2201)
- **Build:**
  - Fix GitHub Pages deploy on master [#2160](https://github.com/patternfly/patternfly/pull/2160)
  - Remove GitHub Pages deploy [#2166](https://github.com/patternfly/patternfly/pull/2166)
- **Workspace:** Fix codepen button title. [#2151](https://github.com/patternfly/patternfly/pull/2151)

## 2019.06 release notes (2019-08-13)
Packages released:
- [@patternfly/patternfly@v2.26.1](https://www.npmjs.com/package/@patternfly/patternfly/v/2.26.1)

### Components
- **Alert:** Added default alert ([#2107](https://github.com/patternfly/patternfly/pull/2107))
- **Data list:** Gave Data List Demo a header ([#2083](https://github.com/patternfly/patternfly/pull/2083))
- **Divider:** Updated example for the li example of the divider, set the type from div to li ([#2089](https://github.com/patternfly/patternfly/issues/2089))
- **Drawer demo:** Removed drawer.scss. The drawer demo used to add a sass file. We removed this since demos should not have their own distributed css. ([#2138](https://github.com/patternfly/patternfly/issues/2138))
- **Charts:** Added color to black range, shift vars ([#2094](https://github.com/patternfly/patternfly/pull/2094))
- **Logo:** PF logo missing width attribute ([#2101](https://github.com/patternfly/patternfly/pull/2101))
- **Page:** Took main elements grid-area value out of custom property ([#2137](https://github.com/patternfly/patternfly/pull/2137))
- **Popover:** Ensure content does not overlap close button ([#2129](https://github.com/patternfly/patternfly/pull/2129))
- **Radio:** Gave unique names to fix a11y ([#2088](https://github.com/patternfly/patternfly/pull/2088))
- **Select:** Added a disable modifier ([#2028](https://github.com/patternfly/patternfly/pull/2028))

### Chore
- **A11y:** Ensure SkipToContent sends focus to page content ([#2058](https://github.com/patternfly/patternfly/pull/2125))
- **Build:** Moved to circle ci
  - Replaces .travis.yml with .circleci/config.yml and split up test steps to allow testing in parallel and nicer Github status checks
  - Fixes our our a11y Selenium script to not have Travis integrations
  - Uploads PR previews to https://surge.sh on every pushed commit (no need for Netlify to also build a preview)
  - Still updates the Github Pages with a new npm run publish:docs target on merges to master ([#2121](https://github.com/patternfly/patternfly/pull/2121))
  - Temporarily disable problematic ci checks ([#2125](https://github.com/patternfly/patternfly/pull/2125))
- **Demos:** Use unique ID used in alert and cardview demos ([#2106](https://github.com/patternfly/patternfly/pull/2106))
- **Notes:** Format release notes using uls
- **Page:** Updated demo main section copy ([#2092](https://github.com/patternfly/patternfly/pull/2092))

## 2019.05 release notes (2019-07-24)
Packages released:
- [@patternfly/patternfly@v2.23.0](https://www.npmjs.com/package/@patternfly/patternfly/v/2.23.0)

### Components
- **About modal:** Updated break-word on content area instead of break-all ([#2035](https://github.com/patternfly/patternfly/pull/2035))
- **App launcher:**
  - Added button example to menu item ([#2006](https://github.com/patternfly/patternfly/pull/2006))
  - Added support for right and top alignment ([#2081](https://github.com/patternfly/patternfly/pull/2081))
- **Charts:**
  - Adjusted chart vars for react-charts ([#2020](https://github.com/patternfly/patternfly/pull/2020), [#2024](https://github.com/patternfly/patternfly/pull/2024))
  - Adjusted 3 chart tooltip color vars and adds two new ones. This will ensure tooltips can be seen over the current background color. ([#2038](https://github.com/patternfly/patternfly/pull/2038))
  - Added individual padding vars for donut charts ([#2068](https://github.com/patternfly/patternfly/pull/2068))
- **Chip:** updated padding on chip label ([#2063](https://github.com/patternfly/patternfly/pull/2063))
- **Divider:** Added divider component ([#2080](https://github.com/patternfly/patternfly/pull/2080))
- **Drawer:** Added drawer component ([#2069](https://github.com/patternfly/patternfly/pull/2069))
- **Dropdown:**
  - Added space-between when width of dropdown grows ([#2050](https://github.com/patternfly/patternfly/pull/2050))
  - Centered plain button content ([#2071](https://github.com/patternfly/patternfly/pull/2071))
- **Icons:** Changed selector that wraps extend for lower specificity ([#2018](https://github.com/patternfly/patternfly/pull/2018))
- **Login:** Unset the link color text so that its white. ([#2032](https://github.com/patternfly/patternfly/pull/2032))
- **Navigation:** Changed max-height to 100% for subnav  ([#2061](https://github.com/patternfly/patternfly/pull/2061))
- **Options menu:** Centered plain button content ([#2071](https://github.com/patternfly/patternfly/pull/2071))
- **Page:** Removed toggle from horizontal nav page demo ([#2004](https://github.com/patternfly/patternfly/pull/2004))
- **Pagination:** Added disabled variation ([#2015](https://github.com/patternfly/patternfly/pull/2015))
- **Select:**
  - Added plain variation ([#2053](https://github.com/patternfly/patternfly/pull/2053))
  - Updated class selector, stacking context for typeahead input ([#2075](https://github.com/patternfly/patternfly/pull/2075))
- **Switch:** Removed dependency on font-size for switch's height ([#2049](https://github.com/patternfly/patternfly/pull/2049))
- **Table:**
  - Renamed data table to table ([#2051](https://github.com/patternfly/patternfly/pull/2051))
  - Wrapped button icons in demo with button icon class to add space ([#1978](https://github.com/patternfly/patternfly/pull/1978))
  - Fixed data-label attr in table demos ([#2060](https://github.com/patternfly/patternfly/pull/2060))
  - Added example of pf-m-wrap modifier for use in thead cells ([#2065](https://github.com/patternfly/patternfly/pull/2065))
- **Tooltip:** Broke words in a place to fit in tooltip ([#2033](https://github.com/patternfly/patternfly/pull/2033))

### Other
- **Red Hat font:** Added opt-in option to use Red Hat font ([#1813](https://github.com/patternfly/patternfly/pull/1813))
- **Experimental features:** Added experimental feature support ([#2031](https://github.com/patternfly/patternfly/pull/2031))

## 2019.04 release notes (2019-07-02)
Packages released:
- [@patternfly/patternfly@v2.17.0](https://www.npmjs.com/package/@patternfly/patternfly/v/2.17.0)

### Components
- **About modal box:** Removed the title classes from the strapline paragraph. Changed strapline `font-size` to 14px. ([#1951](https://github.com/patternfly/patternfly/pull/1951))
- **About modal, app launcher, backdrop, context selector, datalist, dropdown, form control, input group, modal, nav, options menu, select, table, tabs, wizard:**
  - Re-do z-index system so components overlap one another properly ([#1901](https://github.com/patternfly/patternfly/pull/1901))
- **Accordion, clipboard copy, data list, dropdown, expandable, nav, options menu, select, table, wizard:**
  - Added expanded/collapsed arrow rotation in components ([#1932](https://github.com/patternfly/patternfly/pull/1932))
- **Accordion:**
  - Add variation of accordion that uses divs and headings instead of definition list [(#1990](https://github.com/patternfly/patternfly/pull/1990))
- **Alert group:**
  - Renamed example and reworded docs [(#1930](https://github.com/patternfly/patternfly/pull/1930))
- **App launcher:**
  - Used `--pf-global--icon--FontSize--lg` for the icon size. Reduced height of expanded demo  ([#1935](https://github.com/patternfly/patternfly/pull/1935))
  - Added support for sections/icons/divider ([#1916](https://github.com/patternfly/patternfly/pull/1916))
- **Background image:**
  - Reduce size of background image ([#1936](https://github.com/patternfly/patternfly/pull/1936))
- **Card:**
  - Added compact variation ([#1975](https://github.com/patternfly/patternfly/pull/1975))
- **Charts:**
  - Added remaining variables ([#1863](https://github.com/patternfly/patternfly/pull/1863))
- **Copy to Clipboard:**
  - fixed focus ring and add content editable to expanded ([#1896](https://github.com/patternfly/patternfly/pull/1896))
- **Datalist:**
  - Added an example using a heading in the primary content section. ([#1870](https://github.com/patternfly/patternfly/pull/1870))
- **Dropdown:**
  - Increased height of examples that overflow ([#1965](https://github.com/patternfly/patternfly/pull/1965))
- **Expandable:**
  - Added type to the button ([#1982](https://github.com/patternfly/patternfly/pull/1982))
- **Form:**
  - Added element for form label text, make it bold ([#1952](https://github.com/patternfly/patternfly/pull/1952))
- **Form, login, wizard:**
  - Fixed order of variables which was causing the IE conversion script to generate undefined. Also removed a login variable that was not needed. This was also causing the IE scripts to generate undefined. ([#1871](https://github.com/patternfly/patternfly/pull/1871)).
- **Nav:**
  - Changed breakpoint to fix bug ([#1918](https://github.com/patternfly/patternfly/pull/1918))
- **Options menu:**
  - Added disabled variation ([#1973](https://github.com/patternfly/patternfly/pull/1973))
- **Progress:**
  - Changed the font weight on sm variation ([#1974](https://github.com/patternfly/patternfly/pull/1974))
- **Table:**
  - Added an extra breakpoint. In the data table sortable demo the table needed a larger breakpoint, so that the rows don't overflow outside of their container ([#1880](https://github.com/patternfly/patternfly/pull/1880))
  - Added word-wrap to td's in table, now wraps when on mobile size. ([#1928](https://github.com/patternfly/patternfly/pull/1928))
  - Made column headers bold ([#1949](https://github.com/patternfly/patternfly/pull/1949))

### Other
- Added ie11 to build ([#1876](https://github.com/patternfly/patternfly/pull/1876))
- Updated the window size used for the browser that our a11y audit is run against ([#1911](https://github.com/patternfly/patternfly/pull/1911))

## 2019.03 RC2.1 release notes (2019-06-11)
Packages released:
- [@patternfly/patternfly@v2.12.5](https://www.npmjs.com/package/@patternfly/patternfly/v/2.12.5)

### Components
- **About Modal Box:**
  - Addressed overflow of about modal [#1902](https://github.com/patternfly/patternfly/pull/1902)
- **Accordion:**
  - Removed need for class on toggle icon ([#1889](https://github.com/patternfly/patternfly/pull/1889))
- **Button:**
  - Added class around the icon to fix bug ([#1890](https://github.com/patternfly/patternfly/pull/1890))
- **Charts:**
  - Added css vars ([#1868](https://github.com/patternfly/patternfly/pull/1868))
- **Datalist:**
  - Added breakpoints for actions ([#1886](https://github.com/patternfly/patternfly/pull/1886))
- **Inline alert:**
  - We had previously applied `font-size` to the inline alerts via `.pf-c-alert__icon > i`, which didn’t work in React since the icon is an svg. We applied `font-size` to `.pf-c-alert__icon` instead. Also, the inline warning icon was fixed because it was wider than the others ([#1909](https://github.com/patternfly/patternfly/pull/1909))
- **Notification Badge:**
  - Added notification badge ([#1862](https://github.com/patternfly/patternfly/pull/1862))
- **Switch:**
  - Added focus indicator [(#1874](https://github.com/patternfly/patternfly/pull/1874))
- **Table:**
  - Media query now matches border. Fixed the table so that when the table breaks to grid form, the border width changes to 8px, and the border color matches the background color. ([#1881](https://github.com/patternfly/patternfly/pull/1881))

## 2019.02 release notes (2019-05-28)
Packages released:
- [@patternfly/patternfly@v2.8.2](https://www.npmjs.com/package/@patternfly/patternfly/v/2.8.2)

### Components
- **Alert:**
  - Adjusted warning alert webfont icon font-size ([#1805](https://github.com/patternfly/patternfly/pull/1805))
  - Added in-line modifier ([#1775](https://github.com/patternfly/patternfly/pull/1775))
- **Card:**
  - Added a wrapper for the Actions in the top right, so that content wraps around it - similar to the Popover component. Added a logo/img wrapper that is always left aligned. ([#1745](https://github.com/patternfly/patternfly/pull/1745))
  - Removed flex from header ([#1817](https://github.com/patternfly/patternfly/pull/1817))
  - Updated font sizes. Card body and footer text size were changed to be 14px by default and header was changed to 16px. ([#2103](https://github.com/patternfly/patternfly/pull/2103))
  - Card Demo: Added demo ([#1716](https://github.com/patternfly/patternfly/pull/1716))
- **Content:**
  - Changed margin bottom for small element ([#1843](https://github.com/patternfly/patternfly/pull/1843))
- **Form/login/wizard:**
  - Fixed undefined issues with IE11 script ([#1871](https://github.com/patternfly/patternfly/pull/1871))
- **Nav:**
  - Fixed horizontal nav spacing, background color ([#1798](https://github.com/patternfly/patternfly/pull/1798))
  - Updated aria-label in examples ([#1783](https://github.com/patternfly/patternfly/pull/1783))
- **Pagination:**
  - Updated options menu aria-label ([#1782](https://github.com/patternfly/patternfly/pull/1782))
- **Popover:**
  - Scoped title class to popover component ([#1857](https://github.com/patternfly/patternfly/pull/1857))
- **Table:**
  - Remove min-height from buttons in expansion toggle ([#1818](https://github.com/patternfly/patternfly/pull/1818))
- **Wizard:**
  - Fixed wizard closing tag ([#1803](https://github.com/patternfly/patternfly/pull/1803))
  - Set pf-c-wizard__main to grow and fill available height ([#1781](https://github.com/patternfly/patternfly/pull/1781))

### Layouts
- **Gutter:**
  - Updated mobile gutter spacing in layouts that have gutters ([#1829](https://github.com/patternfly/patternfly/pull/1829))

### Other
- **Charts:**
  - Added CSS Variables for Charts ([#1846](https://github.com/patternfly/patternfly/pull/1846))
- **Docs (global):**
  - Updated docs and examples to use sentence case ([#1796](https://github.com/patternfly/patternfly/pull/1796))
- **Guidelines:**
  - Added punctuation, formatting ([#1810](https://github.com/patternfly/patternfly/pull/1810))
