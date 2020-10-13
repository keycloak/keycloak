# Coding Guidelines

## Typescript


The Keycloak UI projects uses best practices based off the official [React TypeScript Cheat sheet](https://react-typescript-cheatsheet.netlify.app/), with modifications for this project. The React TypeScript Cheat sheet is maintained and used by developers through out the world, and is a place where developers can bring together lessons learned using TypeScript and React.

### Imports

Since we are using TypeScript 4.x + for this project, default imports should conform to the new standard set forth in TypeScript 2.7:

```javascript
import React from "react";
import ReactDOM from "react-dom";
```

For imports that are not the default import use the following syntax:

```javascript
import { X1, X2, ... Xn } from "package-x";
```

### Props

For props we are using **type** instead of **interfaces**. The reason to use types instead of interfaces is for consistency between the views and because it"s more constrained (See [Types or Interfaces](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/types_or_interfaces) for more clarification). By using types we are ensuring that we will not deviate from the agreed upon [contract](https://dev.to/reyronald/typescript-types-or-interfaces-for-react-component-props-1408).

The following is an example of using a type for props:

```javascript
import React, { ReactNode } from "react"

...

export type ExampleComponentProps = {
    message: string;
    children: ReactNode;
}
```

### State objects should be types

When maintaining state for a component that requires it's state to be defined by an object, it is recommended that you use a [type instead of an interface](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/types_or_interfaces).  For example if you need to maintain the currentApiId and isExpanded in a single object you can do the following:

```javascript
type ApiDrawerState = {
  currentApiId: string,
  isExpanded: boolean,
};
```

### State management

We have made a conscious decision to stay away from state management technologies such as Redux.  These overarching state management schemes tend to be overly complex and encourage dumping everything into the global state.  
 
Instead, we are following a simple philosophy that state should remain close to where it is used and moved to a wider scope only as truly needed.  This encourages encapsulation and makes management of the state much simpler.
 
The way this plays out in our application is that we first prefer state to remain in the scope of the component that uses it.  If the state is required by more than one component, we move to a more complex strategy for management of that state.  In other words, in order of preference, state should be managed by:
1. Storing in the component that uses it.
2. If #1 is not sufficient, [lift state up](https://reactjs.org/docs/lifting-state-up.html).
3. If #2 is not sufficient, try [component composition](https://reactjs.org/docs/context.html#before-you-use-context).
4. If #3, is not sufficient, use a [global context](https://reactjs.org/docs/context.html).
 
A good tutorial on this approach is found in [Kent Dodds’ blog](https://kentcdodds.com/blog/application-state-management-with-react).

### Interfaces

Interfaces should be used for all public facing API definitions. A table describing when to use interfaces vs. types can be found [here](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/types_or_interfaces).

### Function Components

This project uses function components and hooks over class components. When coding function components in typescript, a developer should include any specific props that they need.

```javascript
import React, { FunctionComponent } from "react";

...

export const ExampleComponent: FunctionComponent<ExampleComponentProps> = ({ message, children }: ExampleComponentProps) => (
  <ReactFragment>
    <div>{message}</div>
    <div>{children}</div>
  </<ReactFragment>>
);
```

For components that do not have any additional props an empty object should be used instead:

```javascript
import React, { FunctionComponent } from "react";

...

export const ExampleNoPropsComponent: FunctionComponent<{}> = () => (
  <div>Example Component with no props</div>
);
```

Additional details around function components can be found [here](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/function_components).

### Hooks

When using hooks with Typescript there are few recommendations that we follow below. Additional recommendations besides the ones mentioned in this document can be found [here](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/hooks).

### Inference vs Types for useState

Currently we recommend using inference for the primitive types booleans, numbers, and strings when using useState. Anything other then these 3 types should use a declarative syntax to specify what is expected. For example the following is an example of how to use inference:

```javascript
const [isEnabled, setIsEnabled] = useState(false);
```

Here is an example how to use a declarative syntax. When using a declarative syntax, if the value can be null, that will also need to be specified:

```javascript
const [user, setUser] = useState<IUser | null>(null);

...

setUser(newUser);


```

#### useReducers

When using reducers make sure you specify the [return type and not use inference](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/hooks#usereducer).

#### useEffect

For useEffect only [return the function or undefined](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/hooks#useeffect).

#### useRef

When using useRef there are two options with Typescript. The first one is when creating a [read-only ref](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/hooks#useref).

```javascript
const refExample = useRef<HTMLElement>(null!);
```

By passing in null! it will prevent Typescript from returning an error saying refExample maybe null.

The second option is for creating [mutable refs](https://react-typescript-cheatsheet.netlify.app/docs/basic/getting-started/hooks#useref) that you will manage.

```javascript
const refExampleMutable = (useRef < HTMLElement) | (null > null);
```

### Additional Typescript Pointers

Besides the details outlined above a list of recommendations for Typescript is maintained by several Typescript React developers [here](https://react-typescript-cheatsheet.netlify.app/). This is a great reference to use for any additional questions that are not outlined within the coding standards.

## CSS

We use custom CSS in rare cases where PatternFly styling does not meet our design guidelines. If styling needs to be added, we should first check that the PatternFly component is being properly built and whether a variant is already provided to meet the use case. Next, PatternFly layouts should be used for most positioning of components. For one-off tweaks (e.g. spacing an icon slightly away from the text next to it), a PatternFly utility class should be used. In all cases, PatternFly variables should be used for colors, spacing, etc. rather than hard coding color or pixel values.

We will use one global CSS file to surface customization variables. Styles particular to a component should be located in a .CSS file within the component’s folder. A modified BEM naming convention should be used as detailed below.

### Location of files, location of classes

* Global styling should be located…?  *./public/index.css*.

* The CSS relating to a single component should be located in a file within each component’s folder.

### Naming CSS classes

PatternFly reference https://pf4.patternfly.org/guidelines#variables

For the Keycloak admin console, we modify the PatternFly convention to namespace the classes and variables to the Keycloak packages.

**Class name**
```css
.keycloak-admin--block[__element][--modifier][--state][--breakpoint][--pseudo-element]
```

**Examples of custom CSS classes**
```css
// Modification to all data tables throughout Keycloak admin 
.keycloak-admin--data-table {
...
}

// Data tables throughout keycloak that are marked as compact 
.keycloak-admin--data-table--compact {
...
}

// Child elements of a compact data-table
// Don’t increase specificity with a selector like this: 
// .keycloak-admin--data-table--compact .data-table-item
// Instead, follow the pattern for a single class on the child
.keycloak-admin--data-table__data-table-item--compact {
...
}

// Compact data table just in the management console at the lg or higher breakpoint
.keycloak-admin--data-table--compact--lg {
...
}
```
### Naming CSS custom properties and using PatternFly’s custom properties

Usually, PatternFly components will properly style components. Sometimes problems with the spacing or other styling indicate that a wrapper component is missing or that components haven’t been put together quite as intended. Often there is a variant of the component available that will accomplish the design.

However, there are other times when modifications must be made to the styling provided by PatternFly, or when styling a custom component. In these cases, PatternFly custom properties (CSS variables) should be used as attribute values. PatternFly defines custom properties for colors, spacing, border width, box shadow, and more. Besides a full color palette, colors are defined specifically for borders, statuses (success, warning, danger, info), backgrounds, etc.

These values can be seen in the [PatternFly design guidelines](https://www.patternfly.org/v4/design-guidelines/styles/colors) and a [full listing of variables](https://www.patternfly.org/v4/documentation/overview/global-css-variables) can be found in the documentation section.

For the Keycloak admin console, we modify the PatternFly convention to namespace the classes and variables to the Keycloak packages.

**Custom property**
```css
--keycloak-admin--block[__element][--modifier][--state][--breakpoint][--pseudo-element]--PropertyCamelCase
```

**Example of a CSS custom property**
```css
// Modify the height of the brand image
--keycloak-admin--brand--Height: var(--pf-global--spacer--xl); 
```

**Example**
```css
// Don’t increase specificity
// Don’t use pixel values
.keycloak-admin--manage-columns__modal .pf-c-dropdown {
   margin-bottom: 24px
}

// Do use a new class
// Do use a PatternFly global spacer variable
.keycloak-admin--manage-columns__dropdown {
   margin-bottom: var(--pf-global--spacer--xl);
}
```
### Using utility classes

Utility classes can be used to add specific styling to a component, such as margin-bottom or padding. However, their use should be limited to one-off styling needs.  

For example, instead of using the utility class for margin-right multiple times, we should define a new Keycloak admin console class that adds this *margin-right: var(--pf-global--spacer--sm);* and in this example, the new class can set the color appropriately as well.

**Using a utility class **
```css
switch (titleStatus) {
   case "success":
     return (
       <>
         <InfoCircleIcon
           className="pf-u-mr-sm" // utility class 
           color="var(--pf-global--info-color--100)"
         />{" "}
         {titleText}{" "}
       </>
     );
   case "failure":
     return (
       <>
         <InfoCircleIcon
           className="pf-u-mr-sm"
           color="var(--pf-global--danger-color--100)"
         />{" "}
         {titleText}{" "}
       </>
     );
 } 
 ``` 
**Better way with a custom class**
```css
switch (titleStatus) {
   case "success":
     return (
       <>
         <InfoCircleIcon
           className="keycloak-admin--icon--info" // use a new keycloak class 
         />{" "}
         {titleText}{" "}
       </>
     );
   case "failure":
     return (
       <>
         <InfoCircleIcon
           className="keycloak-admin--icon--info"
         />{" "}
         {titleText}{" "}
       </>
     );
 }
```

## Resources

* [PatternFly Docs](https://www.patternfly.org/v4/)
* [Katacoda PatternFly tutorials](https://www.patternfly.org/v4/documentation/react/overview/react-training)
* [PatternFly global CSS variables](https://www.patternfly.org/v4/documentation/overview/global-css-variables)
* [PatternFly CSS utility classes](https://www.patternfly.org/v4/documentation/core/utilities/accessibility)
* [React Typescript Cheat sheet](https://react-typescript-cheatsheet.netlify.app/)