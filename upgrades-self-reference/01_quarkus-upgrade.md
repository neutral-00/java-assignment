There was communication issue between quarkus test container and docker. 
Looks like quarkus was a bit outdated as per my local docker - 29.2.0 

```bash
❯ quarkus update --stream=3.15
[INFO] Error stacktraces are turned on.
[INFO] Scanning for projects...
[INFO]
[INFO] ----------------< com.inventorix:java-code-assignment >-----------------
[INFO] Building java-code-assignment 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- quarkus-maven-plugin:3.31.1:update (default-cli) @ java-code-assignment ---
[WARNING] quarkus:update goal is experimental, its options and output might change in future versions
[INFO] Looking for the newly published extensions in registry.quarkus.io
[INFO] Detected project Java version: 17
[INFO]
[INFO] ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
[INFO]
[INFO] Suggested Quarkus platform BOM updates:
[INFO]  ~       io.quarkus.platform:quarkus-bom:pom:[3.13.3 -> 3.15.7]
[INFO]
[INFO] Suggested extensions updates for 'io.quarkus.platform:quarkus-bom':
[INFO]  ✔       io.quarkus:quarkus-jdbc-postgresql (synced with BOM)
[INFO]  ✔       io.quarkus:quarkus-hibernate-orm-panache (synced with BOM)
[INFO]  ✔       io.quarkus:quarkus-rest-jackson (synced with BOM)
[INFO]
[INFO] Suggested extension updates from other origins:
[INFO]  ~       io.quarkiverse.openapi.generator:quarkus-openapi-generator-server:[2.4.7 -> 2.8.2-lts]
[INFO]
[INFO] ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
[INFO]
[INFO]
[INFO] Resolving recipes from 'io.quarkus:quarkus-update-recipes':
[INFO] * matching recipes directory 'core' found:
[INFO]          - no matching recipes.
[INFO] => 0 specific recipes found
[INFO]
[INFO] We have generated a recipe file to update your project (version updates + specific recipes):
[INFO] target/rewrite/rewrite.yaml
[INFO]

Do you want to apply the generated update recipes with OpenRewrite? ([y]es, [n]o, [d]ry-run + [Enter])

y
[INFO]
[INFO]
[INFO]  ------------------------------------------------------------------------
[INFO]
[INFO] Update in progress (this may take a while):
[INFO]   - executing OpenRewrite 6.26.0 command...
[INFO]   - executing process-sources command...
[INFO] 
[INFO] Rewrite process completed. Logs can be found at: target/rewrite/rewrite.log
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  03:38 min
[INFO] Finished at: 2026-01-30T10:33:18+05:30
[INFO] ------------------------------------------------------------------------
```

I had to upgrade further to 3.27

```bash
quarkus update --stream=3.27
```