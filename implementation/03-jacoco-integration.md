Since you ran the tests from within **IntelliJ IDEA**, you are likely seeing the results from the **IntelliJ IDEA native coverage runner**.

However, in the world of **Quarkus**, there is a more specialized tool you should be aware of for "official" reports: **JaCoCo**.

### 1. The Tool You Just Used: IntelliJ IDEA Runner

When you right-click and select **"Run with Coverage"**, IntelliJ uses its own bundled coverage engine.

* **How it works:** It attaches a dynamic agent to the JVM that monitors which lines of code are touched during the test.
* **Visuals:** It gives you the immediate **green** (covered), **red** (missed), and **yellow** (partial) highlights in your editor gutter.
* **The "Doubt":** IntelliJ's runner is great for a quick check, but it can sometimes struggle with **Quarkus** because Quarkus does a lot of bytecode transformation and "magic" at startup that can confuse standard coverage agents.

### 2. The Recommended Tool: `quarkus-jacoco`

For accurate coverage in a Quarkus app—especially for your assignment—you should use the **Quarkus JaCoCo extension**. Unlike the generic IntelliJ tool, this extension is designed to understand Quarkus-specific internals.

**To set it up:**

1. Add the extension via your terminal:
```bash
./mvnw quarkus:add-extension -Dextensions="jacoco"

```


2. Run your tests and generate coverage report through Maven:
```bash
./mvnw clean verify

```


3. **Find the report:** Quarkus will generate a high-fidelity HTML report at:
   `target/jacoco-report/index.html`.

Open this index.html file in any web browser (Chrome, Firefox, etc.). You will see a detailed breakdown:

### 3. Comparing the Two

| Feature | IntelliJ Native Coverage | Quarkus JaCoCo Extension |
| --- | --- | --- |
| **Setup** | Built-in (no config) | Needs `pom.xml` dependency |
| **Best For** | Quick feedback while coding | Final assignment reports / CI/CD |
| **Accuracy** | Good for unit tests | Better for `@QuarkusTest` & Integration |
| **Output** | IDE Gutter / Tool Window | HTML, XML, and CSV files |

### Why your coverage might look "off"

If your coverage percentage seems lower than expected even though your tests passed, it’s usually because of:

* **Generated Code:** Quarkus (and your OpenAPI generator) creates classes in `target/generated-sources` that you haven't tested.
* **Partial Branches:** A line might be green, but if you have a complex `if` statement, only one branch might have been executed (look for **yellow** markers in IntelliJ).

**Would you like me to show you how to exclude those generated OpenAPI classes from your coverage report so you only see the code you actually wrote?**

[Measuring code coverage with JaCoCo in Quarkus](https://www.youtube.com/watch?v=kMmijuuY0og)

This video provides a practical demonstration of how to view and interpret code coverage reports, specifically showing how green and red indicators in the IDE help you identify untested logic.