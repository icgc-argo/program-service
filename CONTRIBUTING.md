# CONTRIBUTING

## Table of Contents
* [Protocol Buffers](#protocol-buffers)
   * [1. Service and Message names should be CamelCase](#1-service-and-message-names-should-be-camelcase)
   * [2. Field names should be snake_case](#2-field-names-should-be-snake_case)
   * [3. Always use the boxed variant for a field](#3-always-use-the-boxed-variant-for-a-field)
   * [4. Enum values should be uppercase only](#4-enum-values-should-be-uppercase-only)
   * [5. Pull Requests involving this repository, require <strong>atleast 2 reviewers</strong>](#5-pull-requests-involving-this-repository-require-atleast-2-reviewers)
* [Server Code](#server-code)
   * [1. Do not use @Builder when using @AfterMapping or @BeforeMapping](#1-do-not-use-builder-when-using-aftermapping-or-beforemapping)
   * [2. For JPA entities, always used boxed types.](#2-for-jpa-entities-always-used-boxed-types)
   * [3. Use kebab-case for naming properties](#3-use-kebab-case-for-naming-properties)
* [Notes](#notes)

## Protocol Buffers
### 1. Service and Message names should be `CamelCase`
### 2. Field names should be `snake_case`
### 3. Always use the boxed variant for a field
    
Since all values in a proto3 message are optional, if a primitive value is not explicitly defined, it is assigned a default value. The issue with this, is that the recipient of the message cannot distinguish between an explicitly defined default value or an undefined value. Although there are many solutions for this (i.e using the OneOf method, or defining and managing field flags), the most attractive approach is the `Boxed method`. When an object (i.e non-primitive and non-enum) is defined as a field of a message, the presence of a value for that field can be checked. This is especially convenient for integration with Mapstruct.

  1. **General rules for boxing primitives and enums**
      
      All wrapper messages that box primitive values should be defined with a name ending in `Value`, and should contain a single field called `value`. The following is an example:

      ```
      message StringValue {
        string value = 1;
      }

      message Int32Value {
        int32 value = 1;
      }

      message BoolValue {
        bool value = 1;
      }
      ```

  2. **Rules for primitives**

      For example, the following is incorrect:

      ```
      message Person {
        string first_name = 1;
        string last_name = 2;
        int32 age = 3;
      }
      ```

      The correct message would be:

      ```
      message Person {
        google.protobuf.StringValue first_name = 1;
        google.protobuf.StringValue last_name = 2;
        google.protobuf.Int32Value age = 3;
      }
      ```

  3. **Rules for enumerations**

      When a message contains an undefined enum, by default the first element defined in the enum (i.e ordinal position 0) is assigned. Similar to the primitive case, `boxed` enumeration values should always be used. 
      The following is an example of incorrect usage of an enum

      ```
      message Person {
        EmployeeType employeeType = 1;
      }

      enum EmployeeType{
        DEVELOPER = 0;
        MANAGER = 1;
      }
      ```

      The correct definition for `Person` is the following:

      ```
      message Person {
        EmplyeeTypeValue employeeType = 1;
      }

      message EmployeeTypeValue {
        EmployeeType value = 1;
      }

      enum EmployeeType{
        DEVELOPER = 0;
        MANAGER = 1;
      }
      ```
      
      Using this method, a boolean `hasEmployeeType` method is generated allowing for presence checking and the following assertions to pass:
      
      ```
      # Assert the employeeType field is undefined
      Person p1 = Person.newBuilder().build();
      assertThat(p1.hasEmployeeType()).isFalse();
      
      # Assert the employeeType field is defined
      Person p2 = Person.newBuilder()
                  .setEmployeeType(
                          EmployeeTypeValue.newBuilder()
                          .setValue(EmployeeType.DEVELOPER)
                          .build())
                  .build();
      assertThat(p2.hasEmployeeType()).isTrue();

      ```
       
  4. **Rules for collections**
      
      Since collections are just a container of elements, presence checking is possible even if the elements are primitives. Therefore, the following message definition is acceptable

      ```
      message Person {
        repeated string favourite_fruits = 1;
      }
      ```
    
### 4. Enum values should be uppercase only
### 5. Pull Requests involving this repository, require **atleast 2 reviewers**


## Server Code
### 1. Do not use `@Builder` when using `@AfterMapping` or `@BeforeMapping`

For mutable entities, do not use the `@Builder` annotation when using the `@AfterMapping` or `@BeforeMapping` Mapstruct annotations, instead use the `@lombok.experimental.Accessors(chain=true)` annotation. Mapstruct detects a builder and uses the builder for mapping, instead of the target mutable object. If a method marked with `@AfterMapping` is mapped to the target object (not the builder object), then the method will NOT be called, since `AfterMapping` is called BEFORE the return, meaning only `@AfterMethod` annotated methods with the builder as the target type will be called.

### 2. For JPA entities, always used boxed types.
        
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

### 3. Use kebab-case for naming properties
For example, in application.yml, avoid using `serverAuthPrefix`. Instead use `server-auth-prefix`. All configurations should be using `@ConfigurationProperties`

## License
[AGPL-3.0](./LICENSE)

## Notes
TOC generated by [gh-md-toc](https://github.com/ekalinin/github-markdown-toc)
[Protocol buffer](https://developers.google.com/protocol-buffers/) message definitions for all Argo services
