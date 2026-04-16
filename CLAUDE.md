## Always prefer `final var x = value` style over `Type x = value` for local variable declarations.
- Use `var` for local variable declarations when the type can be easily inferred from the initializer.
- Use `final` for local variables that are not reassigned after initialization to indicate immutability.
- For generics that infer type during initialization avoid `var`.
  - Eg: `final List<String> names = new ArrayList<>();` instead of `final var names = new ArrayList<String>();`
