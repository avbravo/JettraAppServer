# Scoped Dependency Injection in JettraAppServer

JettraAppServer provides a powerful, contextual Dependency Injection (DI) system that manages object lifecycles based on their defined scopes.

## Supported Scopes

You can define the lifecycle of your injected dependencies by annotating the class with one of the following scope annotations from `io.jettra.scoped`:

* `@ApplicationScoped`: A single instance is created for the entire application. It behaves like a singleton.
* `@SessionScoped`: A unique instance is created per user session. Subsequent requests in the same session will reuse this instance.
* `@RequestScoped`: A new instance is created for every HTTP request.
* `@ViewScoped`: A new instance is created and maintained as long as the user interacts with the same view.
* `@ClientScoped`: A new instance is maintained for a specific client (e.g., a specific browser or API consumer).
* `@WindowScoped`: Similar to `ViewScoped`, but maintained across a specific browser window/tab.

## Usage Example

### 1. Define a Scoped Component

Annotate your repository, service, or component with the desired scope:

```java
package com.example.repository;

import io.jettra.scoped.SessionScoped;

@SessionScoped
public class UserCartRepository {
    // This instance will be unique per session
    private List<Item> items = new ArrayList<>();
    
    public void addItem(Item item) {
        items.add(item);
    }
}
```

### 2. Inject the Component

Use the `@Inject` annotation (from `io.jettra.core.inject.annotation.Inject`) to inject your component into an `HttpHandler` or a controller:

```java
package com.example.controller;

import io.jettra.core.inject.annotation.Inject;
import io.jettra.rest.annotations.GET;
import io.jettra.rest.annotations.Path;
import com.example.repository.UserCartRepository;

@Path("/cart")
public class CartController {

    @Inject
    private UserCartRepository cartRepository;

    @GET
    public String viewCart() {
        // cartRepository will automatically be resolved from the current session context
        return "Cart size: " + cartRepository.getItems().size();
    }
}
```

## How It Works Internally

When a request is received, `JettraServer` sets up a `JettraContext` containing the current session information. Before the request handler is executed, the `DependencyInjector` scans the handler for `@Inject` annotations and resolves the dependencies by checking their scope against the current context.

If a dependency is not found in the context (e.g., it's the first time it is requested in a session), the injector instantiates it and stores it in the `JettraContext` for future reuse within that scope.
