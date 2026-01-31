The error "Plugin not found" for **3.2.5** is frustrating because that version definitively exists in Maven Central. This persistence usually points to a **cached failure**—essentially, Maven failed to download it once (perhaps during the version flip) and has now created a `.lastUpdated` file that prevents it from trying again.

Since you are now on **Quarkus 3.15 LTS**, the most stable approach is to align with the modern Maven plugin stream (**3.5.x**) used by recent Quarkus releases to avoid these legacy resolution bugs.

### 1. Update to the Correct Stable Version

Change your `pom.xml` property to **3.5.4**. This version is natively compatible with Quarkus 3.15 and handles modern Java 17+ bytecode better than the 3.2.x line.

```xml
<properties>
    ...
    <surefire-plugin.version>3.5.4</surefire-plugin.version>
    ...
</properties>

```
Also, I had to update the dependencies section
```xml
<dependency>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>${surefire-plugin.version}</version>
    <scope>compile</scope>
</dependency>
```

### 2. Deep-Clean your Local Repository (Ubuntu)

If the red error persists, it's because Maven is looking at a corrupted local folder instead of the internet. Run these commands to force a fresh start:

```bash
# Delete the local cached folders for both plugins
rm -rf ~/.m2/repository/org/apache/maven/plugins/maven-surefire-plugin/
rm -rf ~/.m2/repository/org/apache/maven/plugins/maven-failsafe-plugin/

# Force Maven to download the new 3.5.4 version from scratch
mvn clean install -U -DskipTests

```

*The `-U` flag is critical here as it tells Maven to ignore any "not found" cached states and re-request everything from the central repository.*

### 3. Sync IntelliJ One Last Time

After the terminal command finishes:

1. Go back to IntelliJ.
2. Press **`Ctrl + Shift + O`** (or click the **Reload All Maven Projects** icon in the Maven sidebar).
3. If the plugin is still red, go to **File > Invalidate Caches...**, select **"Clear file system cache and Local History"**, and click **Invalidate and Restart**.

### Why this happens in IntelliJ

IntelliJ maintains its own index of your Maven dependencies. When you change a version string rapidly (e.g., from 3.1.2 to 3.2.5 to 3.5.0), the IDE's internal index can get out of sync with the actual files on your disk. Deleting the `~/.m2` folder for that plugin and running `mvn install -U` forces both Maven and IntelliJ to "see" the reality of the repository again.

**Did the 3.5.4 update resolve the red lines? Once they are gone, your `ProductEndpointTest` should finally be able to execute.**

[Solving Maven Plugin not found Error in IntelliJ IDEA](https://www.youtube.com/watch?v=noRFAeGxKRQ)

This video provides a step-by-step visual guide on resolving persistent Maven plugin errors in IntelliJ, including how to use the plugin registry and cache invalidation.