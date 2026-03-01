#!/bin/bash

# Build the project
echo "Building fitness-db-mcp-server..."
./mvnw clean package -DskipTests

# Run with stdio profile
echo "Starting fitness-db-mcp-server with stdio mode..."
java -jar target/fitness-db-mcp-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=stdio
