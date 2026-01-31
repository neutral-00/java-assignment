# MicroProfile Upgrade

After upgrading quarkus to 3.27 another error pop up:
```text
[ERROR] COMPILATION ERROR :
[INFO] -------------------------------------------------------------
[ERROR] /home/neutral00/gp/fcs-interview-code-assignment-main/java-assignment/target/generated-sources/jaxrs/com/warehouse/api/WarehouseResource.java:[13,52] package org.eclipse.microprofile.openapi.annotations does not exist
[ERROR] /home/neutral00/gp/fcs-interview-code-assignment-main/java-assignment/target/generated-sources/jaxrs/com/warehouse/api/WarehouseResource.java:[20,4] cannot find symbol
  symbol:   class Operation
  location: interface com.warehouse.api.WarehouseResource
[ERROR] /home/neutral00/gp/fcs-interview-code-assignment-main/java-assignment/target/generated-sources/jaxrs/com/warehouse/api/WarehouseResource.java:[25,4] cannot find symbol
```

The compilation errors you're seeing indicate that the **MicroProfile OpenAPI** annotations (like `@Operation`) are missing from your project's classpath.

When you upgraded to **Quarkus 3.27**, the `quarkus-openapi-generator-server` extension (v2.8.2-lts) was triggered to generate your JAX-RS interfaces. These generated files rely on standard MicroProfile OpenAPI annotations to document your API, but the generator does not automatically pull in the annotation library itself as a transitive dependency in all configurations.

### 1. Fix the Missing Dependencies

To resolve this, you need to add the `quarkus-smallrye-openapi` extension. This extension provides the required `org.eclipse.microprofile.openapi.annotations` package and enables the Swagger UI for your project.

Run this command in your Ubuntu terminal:

```bash
./mvnw quarkus:add-extension -Dextensions="quarkus-smallrye-openapi"

```

**Alternatively**, manually add this to the `<dependencies>` section of your `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-openapi</artifactId>
</dependency>

```

### 2. Why this happened during the upgrade

The `quarkus-openapi-generator` uses a "contract-first" approach. In newer versions of the generator (v2.8+), the template for generating server-side code was updated to include more descriptive OpenAPI metadata. If your original project only used the generator for the client or had a very basic setup, it might have been getting by without the full OpenAPI extension—but the new generated code now strictly requires it.

### 3. Verify the Fix

After adding the dependency, clean your build and restart dev mode:

```bash
./mvnw clean quarkus:dev

```

*The `clean` is important here because it forces a regeneration of the source files in `target/generated-sources/jaxrs/` with a fresh classpath check.*

### Summary of what changed:

* **The Error:** `package org.eclipse.microprofile.openapi.annotations does not exist`.
* **The Cause:** The upgraded OpenAPI generator is producing code that uses these annotations, but the library isn't in your `pom.xml`.
* **The Solution:** Adding `quarkus-smallrye-openapi` provides the missing classes and enables automatic API documentation.

[Microprofile OpenAPI with Quarkus Tutorial](https://www.youtube.com/watch?v=H1Hf9NHQjSc)

This video provides a complete walkthrough of setting up MicroProfile OpenAPI with Quarkus, which directly addresses the missing annotation packages in your build.