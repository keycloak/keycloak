# Patterns when creating builders

A builder should be named after the representation they are building, replacing the suffix `Representation` with
`Builder`, for example `RealmRepresentation` builder should be named `RealmBuilder`.

Builders should extend `Builder`, and have the following static methods:

* `TheBuilder create()`
* `TheBuilder update(TheRepresentation rep)`

When applicable the builder can include overloaded methods with commonly set values:

* `TheBuilder create(String commonValue)`

`Builder` contains a number of convenience methods to help implement `Builders`. Typically any methods implemented in 
a builder should be 1-2 lines maximum, which is done by using convenience methods.

## Simple values

Methods that set simple values should use the following syntax:

* `TheBuilder something(String something)`

Setting boolean values should use `Boolean` or `boolean` depending on what the representation does:

* `TheBuilder enabled(Boolean enabled)`

When applicable a convinience method can be added to set the boolean to true:

* `TheBuilder enabled()`

## Complex values

Adding complex values are done through:

* `TheBuilder things(ThingRepresentation... things)`
* `TheBuilder things(ThingBuilder... things)`

In general both representations and builders should be supported.

When applicable adding complex values by passing simple values can be used as well:

* `TheBuilder things(String... things)`

All of the above should append the new complex values to existing values.

If it is required to be able to overwrite complex values that is done by adding:

* `TheBuilder setThings(...)`

If it is required to be able to remove values from complex values, for example removing a list of keys from a map, that
should be done with:

* `TheBuilder removeThings(String... keys)`
