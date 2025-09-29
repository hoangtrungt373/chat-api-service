# Chat API Microservices

A comprehensive real-time messaging API built with Spring Boot microservices architecture. This project provides essential features for seamless and interactive communication including user management, friend relationships, one-on-one messaging, group chats, and real-time notifications.

## 🏗️ Architecture

### Microservices Structure
- **chat-api-common**: Shared utilities, DTOs, and common components
- **chat-api-user-service**: User authentication, registration, and profile management
- **chat-api-friend-service**: Friend requests and relationship management
- **chat-api-chat-service**: Real-time messaging and WebSocket communication
- **chat-api-group-service**: Group chat creation and management
- **chat-api-notification-service**: Real-time notifications and push notifications
- **chat-api-gateway**: API Gateway for routing and load balancing

### Technology Stack
- **Framework**: Spring Boot 3.5.6, Spring Cloud 2023.0.0
- **Databases**: PostgreSQL (relational), MongoDB (document), Redis (cache)
- **Messaging**: Apache Kafka, WebSocket (STOMP)
- **Security**: Spring Security, JWT
- **API Gateway**: Spring Cloud Gateway
- **Monitoring**: Prometheus, Grafana, Jaeger
- **Containerization**: Docker, Docker Compose

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Git

### 1. Clone the Repository
```bash
git clone <repository-url>
cd chat-api-service
```

### 2. Environment Setup
```bash
# Copy environment template
cp application.properties.example src/main/resources/application.properties

# Update configuration values in application.properties
# Set your database credentials, JWT secret, email settings, etc.
```

### 3. Start Infrastructure Services
```bash
# Start databases and supporting services
docker-compose up -d postgres mongodb redis kafka zookeeper

# Wait for services to be healthy (check with docker-compose ps)
```

### 4. Build and Run Services
```bash
# Build all modules
mvn clean install

# Run individual services (in separate terminals)
mvn spring-boot:run -pl chat-api-user-service
mvn spring-boot:run -pl chat-api-friend-service
mvn spring-boot:run -pl chat-api-chat-service
mvn spring-boot:run -pl chat-api-group-service
mvn spring-boot:run -pl chat-api-notification-service
mvn spring-boot:run -pl chat-api-gateway
```

### 5. Start Monitoring (Optional)
```bash
# Start monitoring stack
docker-compose up -d prometheus grafana jaeger
```

## 📋 API Endpoints

### User Service (Port 8081)
- `POST /api/v1/users/register` - User registration
- `POST /api/v1/users/login` - User login
- `GET /api/v1/users/profile` - Get user profile
- `PUT /api/v1/users/profile` - Update user profile
- `POST /api/v1/users/refresh` - Refresh JWT token

### Friend Service (Port 8082)
- `POST /api/v1/friends/request` - Send friend request
- `GET /api/v1/friends/requests` - Get friend requests
- `PUT /api/v1/friends/request/{id}/accept` - Accept friend request
- `DELETE /api/v1/friends/request/{id}/reject` - Reject friend request
- `GET /api/v1/friends` - Get friends list
- `DELETE /api/v1/friends/{friendId}` - Remove friend

### Chat Service (Port 8083)
- `POST /api/v1/messages` - Send message
- `GET /api/v1/messages/{chatId}` - Get chat messages
- `GET /api/v1/chats` - Get user chats
- `POST /api/v1/chats` - Create new chat
- `WebSocket /ws` - Real-time messaging

### Group Service (Port 8084)
- `POST /api/v1/groups` - Create group
- `GET /api/v1/groups` - Get user groups
- `PUT /api/v1/groups/{groupId}` - Update group
- `POST /api/v1/groups/{groupId}/members` - Add member
- `DELETE /api/v1/groups/{groupId}/members/{memberId}` - Remove member

### Notification Service (Port 8085)
- `GET /api/v1/notifications` - Get notifications
- `PUT /api/v1/notifications/{id}/read` - Mark as read
- `DELETE /api/v1/notifications/{id}` - Delete notification

### Gateway (Port 8080)
- All services are accessible through the gateway at `http://localhost:8080`

## 🔧 Configuration

### Database Configuration
```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/chat_api
spring.datasource.username=postgres
spring.datasource.password=password

# MongoDB
spring.data.mongodb.uri=mongodb://admin:password@localhost:27017/chat_messages

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=password
```

### JWT Configuration
```properties
jwt.secret=your-super-secret-jwt-key
jwt.expiration=86400000
jwt.refresh.expiration=604800000
```

### WebSocket Configuration
```properties
websocket.allowed-origins=*
websocket.stomp-endpoint=/ws
websocket.app-destination-prefix=/app
websocket.user-destination-prefix=/user
```

## 🐳 Docker Deployment

### Development Environment
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Production Deployment
```bash
# Build application images
docker build -t chat-api-user-service ./chat-api-user-service
docker build -t chat-api-friend-service ./chat-api-friend-service
# ... build other services

# Deploy with production docker-compose
docker-compose -f docker-compose.prod.yml up -d
```

## 📊 Monitoring

### Prometheus Metrics
- URL: http://localhost:9090
- Collects metrics from all services

### Grafana Dashboard
- URL: http://localhost:3000
- Username: admin
- Password: admin
- Pre-configured dashboards for service monitoring

### Jaeger Tracing
- URL: http://localhost:16686
- Distributed tracing across services

## 🧪 Testing

### Unit Tests
```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl chat-api-user-service
```

### Integration Tests
```bash
# Run integration tests with Testcontainers
mvn verify -Dspring.profiles.active=test
```

### API Testing
```bash
# Use provided Postman collection
# Import chat-api-postman-collection.json
```

## 🔒 Security

### Authentication
- JWT-based authentication
- Refresh token mechanism
- Password hashing with BCrypt

### Authorization
- Role-based access control
- Method-level security
- Resource-level permissions

### Data Protection
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- CORS configuration

## 📈 Performance

### Caching Strategy
- Redis for session management
- Database query result caching
- Message caching for real-time delivery

### Database Optimization
- Connection pooling
- Query optimization
- Indexing strategy

### Load Balancing
- Spring Cloud LoadBalancer
- Circuit breaker pattern
- Retry mechanisms

## 🚀 Deployment

### Environment Variables
```bash
# Database
export DB_USERNAME=postgres
export DB_PASSWORD=password
export MONGODB_URI=mongodb://admin:password@localhost:27017/chat_messages

# Security
export JWT_SECRET=your-super-secret-jwt-key

# Email
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-app-password
```

### Production Checklist
- [ ] Update JWT secret
- [ ] Configure production databases
- [ ] Set up SSL certificates
- [ ] Configure monitoring
- [ ] Set up log aggregation
- [ ] Configure backup strategy

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the API documentation at http://localhost:8080/swagger-ui.html

## 🔄 Changelog

### v0.0.1-SNAPSHOT
- Initial release
- Basic microservices structure
- User management
- Friend relationships
- Real-time messaging
- Group chats
- Notifications
- API Gateway
