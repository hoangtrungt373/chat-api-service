# Maven Dependency Tree Example

## 🌳 **What Happens When You Add Common Module**

### **1. chat-api-common Dependencies**
```xml
<!-- chat-api-common/pom.xml -->
<dependencies>
    <!-- Inherited from parent -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <!-- ... more inherited dependencies -->
    
    <!-- Common-specific dependencies -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
    </dependency>
    <!-- ... more common dependencies -->
</dependencies>
```

### **2. chat-api-chat-service Dependencies**
```xml
<!-- chat-api-chat-service/pom.xml -->
<dependencies>
    <!-- Inherited from parent -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <!-- ... more inherited dependencies -->
    
    <!-- Common module dependency -->
    <dependency>
        <groupId>vn.ttg.roadmap</groupId>
        <artifactId>chat-api-common</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Service-specific dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <!-- ... more service-specific dependencies -->
</dependencies>
```

## 🔄 **Final Dependency Tree for chat-api-chat-service**

```
chat-api-chat-service
├── Inherited from parent:
│   ├── spring-boot-starter-web
│   ├── spring-boot-starter-security
│   ├── spring-boot-starter-validation
│   ├── spring-boot-starter-data-redis
│   ├── spring-boot-starter-actuator
│   ├── spring-cloud-starter-openfeign
│   ├── lombok
│   ├── mapstruct
│   └── testing dependencies
│
├── From chat-api-common (transitive):
│   ├── spring-boot-starter-web (duplicate - resolved by Maven)
│   ├── spring-boot-starter-security (duplicate - resolved by Maven)
│   ├── spring-boot-starter-validation (duplicate - resolved by Maven)
│   ├── spring-boot-starter-data-redis (duplicate - resolved by Maven)
│   ├── spring-boot-starter-actuator (duplicate - resolved by Maven)
│   ├── spring-cloud-starter-openfeign (duplicate - resolved by Maven)
│   ├── lombok (duplicate - resolved by Maven)
│   ├── mapstruct (duplicate - resolved by Maven)
│   ├── testing dependencies (duplicate - resolved by Maven)
│   ├── jjwt-api
│   ├── jjwt-impl
│   ├── jjwt-jackson
│   └── ... other common-specific dependencies
│
└── Service-specific:
    ├── spring-boot-starter-websocket
    ├── spring-boot-starter-data-mongodb
    ├── spring-boot-starter-data-jpa
    ├── postgresql
    ├── mongodb-driver-sync
    └── ... other service-specific dependencies
```

## ⚠️ **Important: Duplicate Dependencies**

Notice that many dependencies appear **twice**:
- Once inherited from parent
- Once from common module

### **Maven's Resolution Strategy:**
1. **Same version**: Maven keeps only one copy
2. **Different versions**: Maven uses "nearest wins" rule
3. **No conflicts**: Maven resolves automatically

## 🎯 **What This Means Practically**

### **1. Access to Common Classes**
```java
// In chat-api-chat-service, you can now use:
import vn.ttg.roadmap.chatapi.common.dto.UserDto;
import vn.ttg.roadmap.chatapi.common.util.JwtUtil;
import vn.ttg.roadmap.chatapi.common.exception.ChatApiException;
// ... any class from common module
```

### **2. Shared Dependencies Available**
```java
// All common module dependencies are available:
import io.jsonwebtoken.Jwts;
import org.springframework.security.core.Authentication;
import org.mapstruct.Mapper;
import lombok.Data;
// ... any dependency from common module
```

### **3. Runtime Classpath**
The final JAR includes:
- All common module classes
- All common module dependencies
- All service-specific dependencies
- All inherited dependencies (resolved once)

## 🔍 **Maven Commands to See This**

### **View Dependency Tree**
```bash
mvn dependency:tree -pl chat-api-chat-service
```

### **View Effective POM**
```bash
mvn help:effective-pom -pl chat-api-chat-service
```

### **View Resolved Dependencies**
```bash
mvn dependency:resolve -pl chat-api-chat-service
```

## 🚀 **Benefits of This Approach**

### **1. Code Reuse**
- Share DTOs, utilities, exceptions across services
- Consistent data structures
- Common business logic

### **2. Dependency Management**
- Common module manages shared dependencies
- Services get all common dependencies automatically
- Version consistency across services

### **3. Modularity**
- Common code is separated from service-specific code
- Easy to update common functionality
- Clear separation of concerns

## ⚠️ **Potential Issues**

### **1. Circular Dependencies**
```xml
<!-- DON'T DO THIS -->
<!-- common module depending on a service -->
<dependency>
    <groupId>vn.ttg.roadmap</groupId>
    <artifactId>chat-api-user-service</artifactId>
</dependency>
```

### **2. Version Conflicts**
```xml
<!-- If common module has different version -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.4.0</version> <!-- Different from parent -->
</dependency>
```

### **3. Bloated Dependencies**
- Services get dependencies they might not need
- Larger JAR files
- More complex dependency tree

## 🎯 **Best Practices**

### **1. Keep Common Module Lean**
- Only include truly common dependencies
- Avoid heavy dependencies in common module

### **2. Use Dependency Management**
```xml
<!-- In parent POM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### **3. Regular Dependency Audit**
```bash
# Check for unused dependencies
mvn dependency:analyze -pl chat-api-chat-service

# Check for duplicate dependencies
mvn dependency:tree -Dverbose -pl chat-api-chat-service
```

## 🔄 **Alternative Approaches**

### **1. Parent-Only Dependencies**
```xml
<!-- All common dependencies in parent -->
<!-- No common module dependency -->
<!-- Services inherit everything from parent -->
```

### **2. Common Module with Minimal Dependencies**
```xml
<!-- Common module only has shared code -->
<!-- No dependencies in common module -->
<!-- Services add their own dependencies -->
```

### **3. BOM (Bill of Materials)**
```xml
<!-- Separate BOM for dependency management -->
<!-- Common module for shared code -->
<!-- Services use both BOM and common module -->
```

## 🎉 **Conclusion**

When you add the common module dependency, you're creating a **transitive dependency chain** that gives your service access to:

1. **All common module classes** (DTOs, utilities, exceptions)
2. **All common module dependencies** (JWT, validation, etc.)
3. **All inherited dependencies** (resolved by Maven)

This is a powerful pattern for microservices architecture, but it requires careful dependency management to avoid conflicts and bloat.
