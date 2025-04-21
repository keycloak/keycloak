> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/prefer-return-this-type** for documentation.

[Method chaining](https://en.wikipedia.org/wiki/Method_chaining) is a common pattern in OOP languages and TypeScript provides a special [polymorphic this type](https://www.typescriptlang.org/docs/handbook/2/classes.html#this-types).
If any type other than `this` is specified as the return type of these chaining methods, TypeScript will fail to cast it when invoking in subclass.

```ts
class Animal {
  eat(): Animal {
    console.log("I'm moving!");
    return this;
  }
}

class Cat extends Animal {
  meow(): Cat {
    console.log('Meow~');
    return this;
  }
}

const cat = new Cat();
// Error: Property 'meow' does not exist on type 'Animal'.
// because `eat` returns `Animal` and not all animals meow.
cat.eat().meow();

// the error can be fixed by removing the return type of `eat` or use `this` as the return type.
class Animal {
  eat(): this {
    console.log("I'm moving!");
    return this;
  }
}

class Cat extends Animal {
  meow(): this {
    console.log('Meow~');
    return this;
  }
}

const cat = new Cat();
// no errors. Because `eat` returns `Cat` now
cat.eat().meow();
```

## Rule Details

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
class Foo {
  f1(): Foo {
    return this;
  }
  f2 = (): Foo => {
    return this;
  };
  f3(): Foo | undefined {
    return Math.random() > 0.5 ? this : undefined;
  }
}
```

### ✅ Correct

```ts
class Foo {
  f1(): this {
    return this;
  }
  f2() {
    return this;
  }
  f3 = (): this => {
    return this;
  };
  f4 = () => {
    return this;
  };
}

class Base {}
class Derived extends Base {
  f(): Base {
    return this;
  }
}
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/prefer-return-this-type": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you don't use method chaining or explicit return values, you can safely turn this rule off.
