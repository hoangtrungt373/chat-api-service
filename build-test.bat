@echo off
echo Building Chat API Microservices...
echo.

echo Step 1: Cleaning previous builds...
call mvn clean -q

echo Step 2: Compiling parent POM...
call mvn compile -pl . -q

echo Step 3: Installing parent POM to local repository...
call mvn install -pl . -q

echo Step 4: Compiling common module...
call mvn compile -pl chat-api-common -q

echo Step 5: Installing common module...
call mvn install -pl chat-api-common -q

echo Step 6: Compiling all modules...
call mvn compile -q

echo.
echo Build completed successfully!
echo.
echo Next steps:
echo 1. Start infrastructure: docker-compose up -d postgres mongodb redis kafka zookeeper
echo 2. Run services: mvn spring-boot:run -pl chat-api-user-service
echo.
pause
