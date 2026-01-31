# Continuous Integration

To set up a professional CI pipeline for your Quarkus project, we will use **GitHub Actions**. This pipeline will trigger on every `push` and `pull_request` to the `main` branch, ensuring that your unit tests and integration tests (using the Postgres Docker setup) pass before code is merged.

Since you are using **Dev Services** (indicated by your properties), GitHub Actions will automatically handle the Docker containers needed for Postgres if we use the standard Ubuntu runner.

### 1. The GitHub Actions Workflow

Create a file at `.github/workflows/maven-ci.yml` in your repository root.

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest

    services:
      # Although Quarkus Dev Services can start Postgres, 
      # having Docker available on the runner is sufficient.
      postgres:
        image: postgres:15
        env:
          POSTGRES_USER: quarkus_test
          POSTGRES_PASSWORD: quarkus_test
          POSTGRES_DB: quarkus_test
        ports:
          - 15432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Build and Test with Maven
      # This runs all your @QuarkusTests, including the ones we wrote
      run: mvn -B clean verify

    - name: Upload JaCoCo Coverage Report
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: jacoco-report
        path: target/site/jacoco/

```

---

### 2. Why this configuration works

* **Service Container:** It mirrors your local Docker environment. By mapping `15432:5432`, the tests will find the database exactly where they expect it based on your `application.properties`.
* **Caching:** The `cache: maven` setting speeds up subsequent runs by storing your dependencies.
* **Verification:** `mvn verify` is better than `mvn test` because it runs both unit tests and integration tests (Failsafe plugin), ensuring your REST endpoints are fully validated.
* **Artifacts:** After the run, you can download the JaCoCo report directly from the GitHub Actions tab to check your coverage progress.

---

### 3. Git Commands to Push Changes

Now that you have your tests and the pipeline file ready, follow these steps in your terminal:

```bash
# 1. Initialize the repo (if not already done)
git init

# 2. Add all files (make sure target/ is in .gitignore!)
git add .

# 3. Commit your progress
git commit -m "feat: add unit tests and CI pipeline with coverage"

# 4. Link to your GitHub repo
git remote add origin https://github.com/NEUTRAL/YOUR_REPO_NAME.git

# 5. Push to main
git branch -M main
git push -u origin main

```

### High Priority Check: Secret Management

Since your `application.properties` contains database credentials, ensure your repository is **Private** if those are sensitive. If this were a production environment, we would use GitHub Secrets, but for a local dev/test setup with `quarkus_test`, this configuration is standard.

**Would you like me to add a step to the pipeline that fails the build if your JaCoCo test coverage falls below a certain percentage (e.g., 80%)?**