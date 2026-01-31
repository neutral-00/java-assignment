# Application Startup Analysis

### Q. What is happening when we run `./mvnw clean quarkus:dev`


The terminal output you provided is a "behind-the-scenes" look at **Quarkus Dev Services**, which is one of the framework's most powerful features for developer productivity.

### 1. What is happening in those logs?

Here is the step-by-step breakdown of your application's startup sequence:

* **Automatic Database Provisioning (Dev Services):** Because you added a PostgreSQL driver but didn't provide a database URL in your configuration, Quarkus realized you need a database to run. It automatically used **Testcontainers** to pull the `postgres:17` image and start a fresh container for you.
* **Database Schema Management (Hibernate):** Once the database was ready, Hibernate took over. It detected your entities (`Product`, `Store`, `warehouse`) and automatically synchronized the database schema.
* **Drop/Create:** It dropped existing tables and recreated them to match your Java code exactly.
* **Data Seeding:** It executed `INSERT` statements to populate your database with initial test data (like the 'TONSTAD' and 'KALLAX' stores). This usually happens because you have an `import.sql` file in your `src/main/resources` folder.


* **Banner and Feature List:** Finally, the "QUARKUS" ASCII art appeared, indicating the app is live. The **Installed features** list confirms that your new `smallrye-openapi` and `swagger-ui` extensions are active and ready to use.

---

### 2. What is the typical entry point of a Quarkus app?

Unlike traditional Java apps that require a `public static void main` method in a specific class, Quarkus handles the "Main" entry point for you in most cases.

#### **The Framework-Managed Entry Point**

In a standard web/REST application like yours, **there is no manual entry point code**. Quarkus generates a hidden main class during the build process that bootstraps the CDI (Contexts and Dependency Injection) container and starts the HTTP server. Your "entry points" are actually your **JAX-RS Resource classes** (annotated with `@Path`) which handle incoming requests.

#### **Custom Entry Points (Command Mode)**

If you are building a CLI tool or need to run specific logic at startup, you can define an explicit entry point in two ways:

1. **The `@QuarkusMain` Annotation:**
   You can create a class that implements `QuarkusApplication`. The `run` method becomes your manual entry point.
```java
@QuarkusMain
public class MyApp implements QuarkusApplication {
    @Override
    public int run(String... args) {
        System.out.println("Do something startup-related here...");
        Quarkus.waitForExit();
        return 0;
    }
}

```


2. **Startup Observers (The "Quarkus Way"):**
   If you just need to run code as soon as the app starts without replacing the main loop, you use an observer:
```java
@ApplicationScoped
public class AppLifecycle {
    void onStart(@Observes StartupEvent ev) {               
        LOGGER.info("The application is starting...");
    }
}

```



### Next Steps for you:

Now that the app is listening on `http://localhost:8080`, you should check your automatically generated documentation:

* **Swagger UI:** Go to [http://localhost:8080/q/swagger-ui/](https://www.google.com/search?q=http://localhost:8080/q/swagger-ui/) to see and test your API endpoints.
* **Dev UI:** Go to [http://localhost:8080/q/dev/](https://www.google.com/search?q=http://localhost:8080/q/dev/) to manage your running Docker container and inspect your application's health.