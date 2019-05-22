# CONTRIBUTING

## 1. Do not use `@Builder` when using `@AfterMapping` or `@BeforeMapping`

For mutable entities, do not use the `@Builder` annotation when using the `@AfterMapping` or `@BeforeMapping` Mapstruct annotations, instead use the `@lombok.experimental.Accessors(chain=true)` annotation. Mapstruct detects a builder and uses the builder for mapping, instead of the target mutable object. If a method marked with `@AfterMapping` is mapped to the target object (not the builder object), then the method will NOT be called, since `AfterMapping` is called BEFORE the return, meaning only `@AfterMethod` annotated methods with the builder as the target type will be called.

## 2. For JPA entities, always used boxed types.
        
Using boxed types, allows for presence checking by Mapstuct and for the bean validator to check for non-nullness when using the `@NotNull` annotation on the field.

For example, the following definition is incorrect:
```java
@Entity
@Table(name = "person")
public class Person{
  
  @NotNull
  @Column(name = "age")
  private int age;
  
}
```

instead it should be defined as 
```java
@Entity
@Table(name = "person")
public class Person{
  
  @NotNull
  @Column(name = "age")
  private Integer age;
  
}
```


