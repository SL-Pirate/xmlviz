## Always prefer `final var x = value` style over `Type x = value` for local variable declarations.
- Use `var` for local variable declarations when the type can be easily inferred from the initializer.
- Use `final` for local variables that are not reassigned after initialization to indicate immutability.
- For generics that infer type during initialization avoid `var`.
  - Eg: `final List<String> names = new ArrayList<>();` instead of `final var names = new ArrayList<String>();`

## Use underscores for unused variables inside lambdas.
- `Eg: exitItem.setOnAction(e -> Platform.exit());` => `exitItem.setOnAction(_ -> Platform.exit());` 

## Use lombok for ****** sake

## Adding new dependencies
- Do not under any circumstance add a new dependency by yourself.
- If a new 3rd party dependency is required, recommend and ask the human to add it for you.
