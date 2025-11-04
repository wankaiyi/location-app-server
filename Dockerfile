# -------- Builder stage: compile the Spring Boot app with JDK 21 (Temurin/OpenJDK) --------
FROM eclipse-temurin:21
WORKDIR /app

# Copy the JAR built externally by Maven
COPY target/*.jar /app/app.jar

# Default port (can be overridden at runtime)
ENV PORT=10088
ENV JAVA_OPTS=""

# Expose port for documentation purposes (optional)
EXPOSE 10088

# Start the application; allow overriding server port and JVM opts
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} -jar /app/app.jar --server.port=${PORT}"]