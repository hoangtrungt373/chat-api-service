# Maven Dependency Optimization Summary

## 🎯 **What Was Optimized**

We moved common dependencies from individual service modules to the parent POM to eliminate duplication and improve maintainability.

## 📋 **Dependencies Moved to Parent POM**

The following dependencies are now declared **once** in the parent POM and inherited by all service modules:

### **Core Spring Boot Dependencies**
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-security` - Security framework
- `spring-boot-starter-validation` - Input validation
- `spring-boot-starter-data-redis` - Redis caching
- `spring-boot-starter-actuator` - Monitoring endpoints

### **Spring Cloud Dependencies**
- `spring-cloud-starter-openfeign` - Service-to-service communication

### **JWT Dependencies**
- `jjwt-api` - JWT API
- `jjwt-impl` - JWT implementation
- `jjwt-jackson` - JWT Jackson support

### **Development Tools**
- `lombok` - Code generation
- `mapstruct` - DTO mapping
- `mapstruct-processor` - MapStruct annotation processor
- `spring-boot-configuration-processor` - Configuration metadata

### **Testing Dependencies**
- `spring-boot-starter-test` - Spring Boot testing
- `spring-security-test` - Security testing
- `testcontainers` - Integration testing

## 🔧 **Service-Specific Dependencies Remaining**

Each service module now only contains dependencies that are **unique** to that service:

### **User Service**
- `spring-boot-starter-data-jpa` - Database access
- `spring-boot-starter-mail` - Email functionality
- `postgresql` - Database driver

### **Friend Service**
- `spring-boot-starter-data-jpa` - Database access
- `postgresql` - Database driver

### **Chat Service**
- `spring-boot-starter-websocket` - WebSocket support
- `spring-messaging` - STOMP messaging
- `spring-boot-starter-data-mongodb` - MongoDB support
- `spring-boot-starter-data-jpa` - Database access
- `spring-kafka` - Message streaming
- `commons-fileupload` - File upload
- `postgresql` - Database driver
- `mongodb-driver-sync` - MongoDB driver

### **Group Service**
- `spring-boot-starter-data-jpa` - Database access
- `commons-fileupload` - File upload
- `postgresql` - Database driver

### **Notification Service**
- `spring-boot-starter-websocket` - WebSocket support
- `spring-messaging` - STOMP messaging
- `spring-boot-starter-data-jpa` - Database access
- `spring-boot-starter-mail` - Email functionality
- `spring-kafka` - Message streaming
- `firebase-admin` - Push notifications
- `postgresql` - Database driver

### **Gateway Service**
- `spring-cloud-starter-gateway` - API Gateway
- `spring-boot-starter-webflux` - Reactive web (instead of Web MVC)
- `spring-cloud-starter-loadbalancer` - Load balancing
- `spring-cloud-starter-circuitbreaker-reactor-resilience4j` - Circuit breaker
- `spring-boot-starter-data-redis-reactive` - Reactive Redis

## ✅ **Benefits of This Optimization**

### **1. Reduced Duplication**
- Eliminated ~15 duplicate dependency declarations
- Single source of truth for common dependencies

### **2. Easier Maintenance**
- Update common dependencies in one place (parent POM)
- Consistent versions across all services

### **3. Cleaner Service POMs**
- Service POMs now focus only on their specific needs
- Easier to understand what each service actually requires

### **4. Better Dependency Management**
- Centralized version management in parent POM
- Clear separation between common and service-specific dependencies

## 🚀 **How to Use**

### **Adding New Common Dependencies**
1. Add the dependency to the parent POM's `<dependencies>` section
2. All service modules will automatically inherit it

### **Adding Service-Specific Dependencies**
1. Add the dependency to the specific service module's POM
2. Only that service will have access to the dependency

### **Updating Dependency Versions**
1. Update the version in the parent POM's `<properties>` section
2. All services using that dependency will get the updated version

## 📊 **Before vs After**

### **Before Optimization**
- **Parent POM**: 4 dependencies
- **Each Service POM**: ~20-25 dependencies
- **Total Duplication**: ~15 common dependencies × 6 services = 90 duplicate declarations

### **After Optimization**
- **Parent POM**: 19 common dependencies
- **Each Service POM**: 3-8 service-specific dependencies
- **Total Duplication**: 0 (all common dependencies centralized)

## 🔍 **Verification**

To verify the optimization worked correctly:

```bash
# Build all modules
mvn clean install

# Check dependency tree for any service
mvn dependency:tree -pl chat-api-user-service

# Verify common dependencies are inherited
mvn dependency:tree -pl chat-api-user-service | grep "spring-boot-starter-web"
```

## 📝 **Notes**

- The `chat-api-common` module was not modified as it serves as a shared library
- All service modules still have access to all common dependencies
- Service-specific dependencies remain in their respective modules
- The optimization maintains the same functionality while improving maintainability
