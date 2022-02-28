---
title: "withouia"
---

### Adding OUIA capabilities to library components


 1. Import: `import { InjectedOuiaProps, withOuiaContext } from  '../withOuia';`
 2. For TS combine the props with the InjectedOuiaProps `class  Switch  extends  React.Component<SwitchProps  &  InjectedOuiaProps>`
 3. Wrap the component in the withOuiaContext higher-order-component
```
const  SwitchWithOuiaContext = withOuiaContext(Switch);
export { SwitchWithOuiaContext as Switch };
```
 4. OUIA props are in `this.props.ouiaContext`
```
const { ouiaContext, ouiaId } = this.props;
<label
	className=""
	htmlFor=""
	{...ouiaContext.isOuia && {
	'data-ouia-component-type':  'Switch',
	'data-ouia-component-id':  ouiaId || ouiaContext.ouiaId
	}}
>my label</label>
```

### Consumer usage
#### Case 1: non-ouia users
```
<Switch  />
```
> No re-render, does not render ouia attributes
#### Case 2: enable ouia through local storage
##### in local storage _ouia: true_
```
<Switch  />
```
> render's ouia attribute **data-ouia-component-type="Switch"**
#### Case 3: enable ouia through local storage and generate id
##### in local storage _ouia: true_
##### in local storage _ouia-generate-id: true_
```
<Switch  />
```
> render's ouia attributes **data-ouia-component-type="Switch" data-ouia-component-id="0"**
#### Case 4: enable ouia through local storage and provide id
##### in local storage _ouia: true_
```
<Switch ouiaId="my_switch_id" />
```
> render's ouia attributes **data-ouia-component-type="Switch" data-ouia-component-id="my_switch_id"**
#### Case 5: enable ouia through context and provide id
##### Note: If context provided _isOuia_ is true and local storage provided _isOuia_ is false, context will win out. Context will also win if its _isOuia_ is false and local storage's is true. Context > local storage
```
import { OuiaContext } from  '@patternfly/react-core';
<OuiaContext.Provider value={{ isOuia: true }}>
	<Switch ouiaId="my_switch_id" />
</OuiaContext.Provider>
```
> render's ouia attributes **data-ouia-component-type="Switch" data-ouia-component-id="my_switch_id"**
