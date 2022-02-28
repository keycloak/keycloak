# Require a consistent member declaration order (member-ordering)

A consistent ordering of fields, methods and constructors can make interfaces, type literals, classes and class
expressions easier to read, navigate and edit.

## Rule Details

This rule aims to standardise the way interfaces, type literals, classes and class expressions are structured.

## Options

This rule, in its default state, does not require any argument, in which case the following order is enforced:

- `public-static-field`
- `protected-static-field`
- `private-static-field`
- `public-instance-field`
- `protected-instance-field`
- `private-instance-field`
- `public-field` (ignores scope)
- `protected-field` (ignores scope)
- `private-field` (ignores scope)
- `static-field` (ignores accessibility)
- `instance-field` (ignores accessibility)
- `field` (ignores scope and/or accessibility)
- `constructor` (ignores scope and/or accessibility)
- `public-static-method`
- `protected-static-method`
- `private-static-method`
- `public-instance-method`
- `protected-instance-method`
- `private-instance-method`
- `public-method` (ignores scope)
- `protected-method` (ignores scope)
- `private-method` (ignores scope)
- `static-method` (ignores accessibility)
- `instance-method` (ignores accessibility)
- `method` (ignores scope and/or accessibility)

The rule can also take one or more of the following options:

- `default`, use this to change the default order (used when no specific configuration has been provided).
- `classes`, use this to change the order in classes.
- `classExpressions`, use this to change the order in class expressions.
- `interfaces`, use this to change the order in interfaces.
- `typeLiterals`, use this to change the order in type literals.

### default

Disable using `never` or use one of the following values to specify an order:

- Fields:  
  `public-static-field`  
  `protected-static-field`  
  `private-static-field`  
  `public-instance-field`  
  `protected-instance-field`  
  `private-instance-field`  
  `public-field` (= public-_-field)  
  `protected-field` (= protected-_-field)  
  `private-field` (= private-_-field)  
  `static-field` (= _-static-field)  
  `instance-field` (= \*-instance-field)  
  `field` (= all)

- Constructors:  
  `public-constructor`  
  `protected-constructor`  
  `private-constructor`  
  `constructor` (= \*-constructor)

- Methods:
  `public-static-method`  
  `protected-static-method`  
  `private-static-method`  
  `public-instance-method`  
  `protected-instance-method`  
  `private-instance-method`  
  `public-method` (= public-_-method)  
  `protected-method` (= protected-_-method)  
  `private-method` (= private-_-method)  
  `static-method` (= _-static-method)  
  `instance-method` (= \*-instance-method)  
  `method` (= all)

Examples of **incorrect** code for the `{ "default": [...] }` option:

```ts
// { "default": ["method", "constructor", "field"] }

interface Foo {
  // -> field
  B: string;

  // -> constructor
  new ();

  // -> method
  A(): void;
}

type Foo = {
  // -> field
  B: string;

  // no constructor

  // -> method
  A(): void;
};

class Foo {
  // -> * field
  private C: string;
  public D: string;
  protected static E: string;

  // -> constructor
  constructor() {}

  // -> * method
  public static A(): void {}
  public B(): void {}
}

const Foo = class {
  // -> * field
  private C: string;
  public D: string;

  // -> constructor
  constructor() {}

  // -> * method
  public static A(): void {}
  public B(): void {}

  // * field
  protected static E: string;
};

// { "default": ["public-instance-method", "public-static-field"] }

// does not apply for interfaces/type literals (accessibility and scope are not part of interfaces/type literals)

class Foo {
  // private instance field
  private C: string;

  // public instance field
  public D: string;

  // -> public static field
  public static E: string;

  // constructor
  constructor() {}

  // public static method
  public static A(): void {}

  // -> public instance method
  public B(): void {}
}

const Foo = class {
  // private instance field
  private C: string;

  // -> public static field
  public static E: string;

  // public instance field
  public D: string;

  // constructor
  constructor() {}

  // public static method
  public static A(): void {}

  // -> public instance method
  public B(): void {}
};
```

Examples of **correct** code for the `{ "default": [...] }` option:

```ts
// { "default": ["method", "constructor", "field"] }

interface Foo {
    // -> method
    A() : void;

    // -> constructor
    new();

    // -> field
    B: string;
}

type Foo = {
    // -> method
    A() : void;

    // -> field
    B: string;
}

class Foo {
    // -> * method
    public static A(): void {}
    public B(): void {}

    // -> constructor
    constructor() {}

    // -> * field
    private C: string
    public D: string
    protected static E: string
}

const Foo = class {
    // -> * method
    public static A(): void {}
    public B(): void {}

    // -> constructor
    constructor() {}

    // -> * field
    private C: string
    public D: string
    protected static E: string
}

// { "default": ["public-instance-method", "public-static-field"] }

// does not apply for interfaces/type literals (accessibility and scope are not part of interfaces/type literals)

class Foo {
    // -> public instance method
    public B(): void {}

    // private instance field
    private C: string

    // public instance field
    public D: string

    // -> public static field
    public static E: string

    // constructor
    constructor() {}

    // public static method
    public static A(): void {}
}

const Foo = class {
    // -> public instance method
    public B(): void {}

    // private instance field
    private C: string

    // public instance field
    public D: string

    // constructor
    constructor() {}

    // public static method
    public static A(): void {}

    // -> protected static field
    protected static: string
}

// { "default": ["public-static-field", "static-field", "instance-field"] }

// does not apply for interfaces/type literals (accessibility and scope are not part of interfaces/type literals)

class Foo {
    // -> public static field
    public static A: string;

    // -> * static field
    private static B: string;
    protected statis C:string;
    private static D: string;

    // -> * instance field
    private E: string;
}

const foo = class {
    // * method
    public T(): void {}

    // -> public static field
    public static A: string;

    // constructor
    constructor(){}

    // -> * static field
    private static B: string;
    protected statis C:string;
    private static D: string;

    // -> * instance field
    private E: string;
}
```

### classes

Disable using `never` or use one of the valid values (see default) to specify an order.

Examples of **incorrect** code for the `{ "classes": [...] }` option:

```ts
// { "classes": ["method", "constructor", "field"] }

// does not apply for interfaces/type literals/class expressions.

class Foo {
  // -> field
  private C: string;
  public D: string;
  protected static E: string;

  // -> constructor
  constructor() {}

  // -> method
  public static A(): void {}
  public B(): void {}
}

// { "classes": ["public-instance-method", "public-static-field"] }

// does not apply for interfaces/type literals/class expressions.

class Foo {
  // private instance field
  private C: string;

  // public instance field
  public D: string;

  // -> public static field
  public static E: string;

  // constructor
  constructor() {}

  // public static method
  public static A(): void {}

  // -> public instance method
  public B(): void {}
}
```

Examples of **correct** code for `{ "classes": [...] }` option:

```ts
// { "classes": ["method", "constructor", "field"] }

// does not apply for interfaces/type literals/class expressions.

class Foo {
  // -> * method
  public static A(): void {}
  public B(): void {}

  // -> constructor
  constructor() {}

  // -> * field
  private C: string;
  public D: string;
  protected static E: string;
}

// { "classes": ["public-instance-method", "public-static-field"] }

// does not apply for interfaces/type literals/class expressions.

class Foo {
  // private instance field
  private C: string;

  // public instance field
  public D: string;

  // -> public static field
  public static E: string;

  // constructor
  constructor() {}

  // public static method
  public static A(): void {}

  // -> public instance method
  public B(): void {}
}
```

### classExpressions

Disable using `never` or use one of the valid values (see default) to specify an order.

Examples of **incorrect** code for the `{ "classExpressions": [...] }` option:

```ts
// { "classExpressions": ["method", "constructor", "field"] }

// does not apply for interfaces/type literals/class expressions.

const foo = class {
  // -> field
  private C: string;
  public D: string;
  protected static E: string;

  // -> constructor
  constructor() {}

  // -> method
  public static A(): void {}
  public B(): void {}
};

// { "classExpressions": ["public-instance-method", "public-static-field"] }

// does not apply for interfaces/type literals/class expressions.

const foo = class {
  // private instance field
  private C: string;

  // public instance field
  public D: string;

  // -> public static field
  public static E: string;

  // constructor
  constructor() {}

  // public static method
  public static A(): void {}

  // -> public instance method
  public B(): void {}
};
```

Examples of **correct** code for `{ "classExpressions": [...] }` option:

```ts
// { "classExpressions": ["method", "constructor", "field"] }

// does not apply for interfaces/type literals/class expressions.

const foo = class {
  // -> * method
  public static A(): void {}
  public B(): void {}

  // -> constructor
  constructor() {}

  // -> * field
  private C: string;
  public D: string;
  protected static E: string;
};

// { "classExpressions": ["public-instance-method", "public-static-field"] }

// does not apply for interfaces/type literals/class expressions.

const foo = class {
  // private instance field
  private C: string;

  // public instance field
  public D: string;

  // -> public static field
  public static E: string;

  // constructor
  constructor() {}

  // public static method
  public static A(): void {}

  // -> public instance method
  public B(): void {}
};
```

### interfaces

Disable using `never` or use one of the following values to specify an order:  
`field`  
`constructor`  
`method`

Examples of **incorrect** code for the `{ "interfaces": [...] }` option:

```ts
// { "interfaces": ["method", "constructor", "field"] }

// does not apply for classes/class expressions/type literals

interface Foo {
  // -> field
  B: string;

  // -> constructor
  new ();

  // -> method
  A(): void;
}
```

Examples of **correct** code for the `{ "interfaces": [...] }` option:

```ts
// { "interfaces": ["method", "constructor", "field"] }

// does not apply for classes/class expressions/type literals

interface Foo {
  // -> method
  A(): void;

  // -> constructor
  new ();

  // -> field
  B: string;
}
```

### typeLiterals

Disable using `never` or use one of the valid values (see interfaces) to specify an order.

Examples of **incorrect** code for the `{ "typeLiterals": [...] }` option:

```ts
// { "typeLiterals": ["method", "constructor", "field"] }

// does not apply for classes/class expressions/interfaces

type Foo = {
  // -> field
  B: string;

  // -> method
  A(): void;
};
```

Examples of **correct** code for the `{ "typeLiterals": [...] }` option:

```ts
// { "typeLiterals": ["method", "constructor", "field"] }

// does not apply for classes/class expressions/interfaces

type Foo = {
  // -> method
  A(): void;

  // -> constructor
  new ();

  // -> field
  B: string;
};
```

## When Not To Use It

If you don't care about the general structure of your classes and interfaces, then you will not need this rule.

## Compatibility

- TSLint: [member-ordering](https://palantir.github.io/tslint/rules/member-ordering/)
