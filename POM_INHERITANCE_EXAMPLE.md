# Maven POM Inheritance Example

## 🏗️ **How Your Project Works**

### **Parent POM (chat-api-service-parent)**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- This is a POM-only project, no JAR produced -->
    <packaging>pom</packaging>
    
    <groupId>vn.ttg.roadmap</groupId>
    <artifactId>chat-api-service-parent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <!-- Properties inherited by ALL children -->
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.5.6</spring-boot.version>
    </properties>
    
    <!-- Dependencies inherited by ALL children -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <!-- ... more common dependencies -->
    </dependencies>
    
    <!-- Child modules -->
    <modules>
        <module>chat-api-user-service</module>
        <module>chat-api-friend-service</module>
        <!-- ... other modules -->
    </modules>
</project>
```

### **Child POM (chat-api-user-service)**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- This produces a JAR file -->
    <packaging>jar</packaging>
    
    <!-- Declare parent -->
    <parent>
        <groupId>vn.ttg.roadmap</groupId>
        <artifactId>chat-api-service-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    
    <artifactId>chat-api-user-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <!-- Only service-specific dependencies -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <!-- Inherits ALL parent dependencies automatically -->
    </dependencies>
</project>
```

## 🔄 **What Happens During Build**

### **1. Maven reads parent POM**
- Sees `<packaging>pom</packaging>` → knows this is a parent/aggregator
- No JAR file is created
- Dependencies and properties are prepared for inheritance

### **2. Maven processes each child module**
- Reads child POM
- Sees `<parent>` declaration → inherits from parent
- Combines parent dependencies + child dependencies
- Creates complete dependency tree
- Builds JAR file (because `<packaging>jar</packaging>`)

### **3. Final result for chat-api-user-service**
The service actually has these dependencies:
```
Inherited from parent:
├── spring-boot-starter-web
├── spring-boot-starter-security
├── spring-boot-starter-validation
├── spring-boot-starter-data-redis
├── spring-boot-starter-actuator
├── spring-cloud-starter-openfeign
├── jjwt-api, jjwt-impl, jjwt-jackson
├── lombok
├── mapstruct, mapstruct-processor
└── testing dependencies

Added by child:
├── spring-boot-starter-data-jpa
├── spring-boot-starter-mail
└── postgresql driver
```

## 📦 **Packaging Types Explained**

### **`<packaging>pom</packaging>`**
- **Purpose**: Parent/aggregator POM
- **Produces**: Nothing (no JAR/WAR/EAR)
- **Use case**: Managing child modules, dependency versions
- **Example**: Your parent POM

### **`<packaging>jar</packaging>`**
- **Purpose**: Java library/application
- **Produces**: `.jar` file
- **Use case**: Most Spring Boot applications
- **Example**: All your service modules

### **`<packaging>war</packaging>`**
- **Purpose**: Web application
- **Produces**: `.war` file
- **Use case**: Traditional web applications
- **Example**: Not used in your project

## 🎯 **Benefits of This Structure**

### **1. DRY Principle (Don't Repeat Yourself)**
```xml
<!-- Instead of this in every service: -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- You declare once in parent, inherit everywhere -->
```

### **2. Centralized Management**
```xml
<!-- Update version in one place -->
<properties>
    <spring-boot.version>3.6.0</spring-boot.version>
</properties>

<!-- All children get the new version -->
```

### **3. Consistent Dependencies**
- All services have the same common dependencies
- No version conflicts between services
- Easier to maintain and upgrade

### **4. Clear Separation**
- Parent: Common dependencies
- Child: Service-specific dependencies
- Easy to understand what each service needs

## 🔧 **Maven Commands**

### **Build entire project**
```bash
mvn clean install
# Builds parent first, then all children
```

### **Build specific module**
```bash
mvn clean install -pl chat-api-user-service
# Builds only user service (and its dependencies)
```

### **See dependency tree**
```bash
mvn dependency:tree -pl chat-api-user-service
# Shows all dependencies (inherited + specific)
```

### **See effective POM**
```bash
mvn help:effective-pom -pl chat-api-user-service
# Shows the final POM after inheritance
```

## 🚀 **Real-World Analogy**

Think of it like a family:

- **Parent POM** = Family rules and common items
  - "Everyone gets a bed, food, and education"
  - "We all use the same family car"
  - "Common house rules apply to everyone"

- **Child POMs** = Individual family members
  - "I need a computer for work" (service-specific)
  - "I need sports equipment" (service-specific)
  - "I inherit the family car and house rules" (inherited)

The parent provides the foundation, children add their specific needs!
