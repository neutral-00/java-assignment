It is perfectly normal to be puzzled by a 0% coverage report when your tests are green, but the reason is quite technical: your tests are currently running as **standard JUnit tests**, but the Quarkus JaCoCo extension is waiting for a **Quarkus-managed test** to activate its coverage agent.

In your screenshot, notice that every element of `LocationGateway` shows 0% coverage. This confirms that while the tests ran, the "sensor" that records code execution was never turned on.

### The Fix: Add `@QuarkusTest`

Because you are using the `quarkus-jacoco` extension, it only "listens" to classes annotated with `@QuarkusTest`. This annotation bootstraps the Quarkus framework for the test, allowing JaCoCo to hook into the bytecode.

Update your `LocationGatewayTest.java` like this:

```java
package com.fulfilment.application.monolith.location;

import io.quarkus.test.junit.QuarkusTest; // Add this import
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest // Add this annotation here
public class LocationGatewayTest {
    // ... rest of your code stays the same
}

```

### Why this is happening

* **Lifecycle Mismatch:** Without `@QuarkusTest`, Maven runs your test in a "plain" Java environment. The JaCoCo agent provided by Quarkus is designed to instrument code only when the Quarkus runtime is active.
* **The `pom.xml` verification:** Your `pom.xml` has `quarkus-jacoco` and `quarkus-junit5`, which is correct. The infrastructure is there, but the "switch" (the annotation) wasn't flipped.

---

### One More Cleanup: Remove the "Plugin Dependency"

While looking at your `pom.xml`, I noticed you still have the `maven-failsafe-plugin` listed inside your `<dependencies>` block:

```xml
<dependency>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>${surefire-plugin.version}</version>
    <scope>compile</scope>
</dependency>

```

**You should delete this specific block.** Now that you have correctly set the `<surefire-plugin.version>` to `3.5.4` in your properties, Maven will find the plugin naturally in the `<build>` section. Keeping it in `<dependencies>` is a "hack" that can cause your final JAR file to include unnecessary build-tool code.

### Summary Checklist to see 100% Coverage:

1. **Remove** the `maven-failsafe-plugin` from the `<dependencies>` section of `pom.xml`.
2. **Add** `@QuarkusTest` to `LocationGatewayTest.java`.
3. **Run** `./mvnw clean verify`.
4. **Refresh** the `index.html` in your browser.
