# Issues

## 1. Quarkus TestContainer using older docker daemon api version 1.32

Found some issue while running `./mvnw quarkus:dev`

```text
2026-01-30 09:04:16,254 ERROR [org.tes.doc.DockerClientProviderStrategy] (build-37) Could not find a valid Docker environment. Please check configuration. Attempted configurations were:
        EnvironmentAndSystemPropertyClientProviderStrategy: failed with exception BadRequestException (Status 400: {"message":"client version 1.32 is too old. Minimum supported API version is 1.44, please upgrade your client to a newer version"}
)
        UnixSocketClientProviderStrategy: failed with exception BadRequestException (Status 400: {"message":"client version 1.32 is too old. Minimum supported API version is 1.44, please upgrade your client to a newer version"}
)

```

### Fix
Upgrading to **Quarkus 3.15 LTS** is a solid choice. It provides long-term stability and, most importantly, resolves your Docker API version mismatch by pulling in a newer version of Testcontainers.

Here is the exact step-by-step process for your **Ubuntu** environment.

### Step 1: Install the Quarkus CLI

The Quarkus CLI is the most reliable way to perform managed upgrades. Open your terminal and run these two commands:

**Install the CLI:**
```bash
sdk install quarkus
```

---

### Step 2: Run the Managed Upgrade

Navigate to your project root (where the `pom.xml` is) and run the update command targeting the **3.15 stream**:

```bash
quarkus update --stream=3.15

```

**What this does:**

* It analyzes your `pom.xml` and updates the `<quarkus.platform.version>` to the latest in the 3.15.x line (e.g., **3.15.7**).
* It uses **OpenRewrite** to automatically fix deprecated configurations in your `application.properties`.
* It fixes common breaking changes in your Java code (like renamed annotations).

---

### Step 3: Verify and Test

Once the update finishes, you can run your failing test. Since 3.15 uses a modern Testcontainers version, it should handle the Docker handshake correctly.

```bash
mvn test -Dtest=ProductEndpointTest

```

---

### Risks and What to Watch For (3.13 → 3.15)

While this is a "minor" jump in Quarkus versions, there are three specific areas to check:

1. **Hibernate ORM 6.6:** Your `pom.xml` indicates you use Hibernate. Version 3.15 introduces **Auto-flush optimizations**. Hibernate is now smarter about when it sends data to the DB before a query. Most apps see a speed boost, but if you have very complex manual transaction logic, verify your data integrity.
2. **OpenTelemetry:** If you use the `quarkus-opentelemetry` extension, be aware that many `quarkus.opentelemetry.*` properties were removed or renamed to follow new industry standards. The `quarkus update` tool usually fixes these automatically.
3. **Dev Services Images:** Quarkus 3.15 bumps the default versions of several "Dev Services" containers. For example, the default PostgreSQL container is now version **16**.

**If the `quarkus update` command fails or asks for confirmation:**
Simply type `y` (yes) to allow it to apply the OpenRewrite recipes. It will create a backup of your files if you are using Git (which you should be!).

## Plugin 'org.apache.maven.plugins:maven-failsafe-plugin:3.1.2' not found 

I could see this issue in pom.xml

### Fix
Update the surefire plugin to 3.5.0 from 3.1.2

Open pom.xml
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
```bash
# Delete the local cached folders for both plugins
rm -rf ~/.m2/repository/org/apache/maven/plugins/maven-surefire-plugin/
rm -rf ~/.m2/repository/org/apache/maven/plugins/maven-failsafe-plugin/

# Force Maven to download the new 3.5.4 version from scratch
mvn clean install -U -DskipTests
```
Press Ctrl-Shift-o