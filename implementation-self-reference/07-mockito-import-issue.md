# Mockito Import Issue

In the `StoreEndpointTest.java` the `mockito` imports were not resolving.

## Solution

Add the below dependency in pom.xml

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5-mockito</artifactId>
    <scope>test</scope>
</dependency>
```