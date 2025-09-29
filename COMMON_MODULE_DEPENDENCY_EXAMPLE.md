# Common Module Dependency Example

## 🎯 **What Happens When You Add Common Module**

### **Before Adding Common Module**
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
    
    <!-- Service-specific -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
</dependencies>
```

### **After Adding Common Module**
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
    
    <!-- Service-specific -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
</dependencies>
```

## 🔄 **Maven's Resolution Process**

### **1. Maven Reads Dependencies**
```
Step 1: Read chat-api-chat-service POM
├── Inherited from parent: spring-boot-starter-web, spring-boot-starter-security, ...
├── Common module: chat-api-common
└── Service-specific: spring-boot-starter-websocket, spring-boot-starter-data-mongodb, ...

Step 2: Resolve chat-api-common
├── Read chat-api-common POM
├── Inherited from parent: spring-boot-starter-web, spring-boot-starter-security, ...
├── Common-specific: jjwt-api, jjwt-impl, jjwt-jackson, ...
└── Build dependency tree

Step 3: Merge Dependencies
├── Remove duplicates (same version)
├── Resolve conflicts (different versions)
└── Create final dependency tree
```

### **2. Final Dependency Tree**
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

## 🎯 **What Your Service Gets Access To**

### **1. Common Module Classes**
```java
// In chat-api-chat-service, you can now use:
import vn.ttg.roadmap.chatapi.common.dto.UserDto;
import vn.ttg.roadmap.chatapi.common.dto.MessageDto;
import vn.ttg.roadmap.chatapi.common.util.JwtUtil;
import vn.ttg.roadmap.chatapi.common.exception.ChatApiException;
import vn.ttg.roadmap.chatapi.common.security.SecurityConfig;
import vn.ttg.roadmap.chatapi.common.mapper.MessageMapper;
```

### **2. All Common Module Dependencies**
```java
// All dependencies from common module are available:
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import lombok.Data;
import lombok.Builder;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import com.fasterxml.jackson.databind.ObjectMapper;
```

### **3. Inherited Dependencies (Resolved)**
```java
// Dependencies inherited from parent are also available:
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.boot.actuator.health.Health;
import org.springframework.cloud.openfeign.FeignClient;
```

## ⚠️ **Important: Duplicate Dependencies**

Notice that many dependencies appear **twice**:
- Once inherited from parent POM
- Once from common module

### **Maven's Resolution Strategy:**
1. **Same version**: Maven keeps only one copy (no duplication)
2. **Different versions**: Maven uses "nearest wins" rule
3. **No conflicts**: Maven resolves automatically

### **Example of Duplicate Resolution:**
```xml
<!-- Parent POM -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.5.6</version>
</dependency>

<!-- Common module -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.5.6</version>
</dependency>

<!-- Maven resolves to: -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.5.6</version>
</dependency>
<!-- Only one copy in final classpath -->
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

### **Check for Duplicate Dependencies**
```bash
mvn dependency:tree -Dverbose -pl chat-api-chat-service
```

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

## 🎉 **Conclusion**

When you add the common module dependency, you're creating a **transitive dependency chain** that gives your service access to:

1. **All common module classes** (DTOs, utilities, exceptions)
2. **All common module dependencies** (JWT, validation, etc.)
3. **All inherited dependencies** (resolved by Maven)

This is a powerful pattern for microservices architecture, but it requires careful dependency management to avoid conflicts and bloat.
