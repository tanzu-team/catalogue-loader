# Stage 1: Build application & generate custom JRE dynamically
FROM bellsoft/hardened-liberica-native-image-kit-container:jdk-25-nik-25-musl AS builder
WORKDIR /build

# Cache Maven dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Build JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Extract layers directly from the built JAR (dynamically finds whatever pom.xml produced)
RUN APP_JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*.original" | head -n 1) && \
    java -Djarmode=tools -jar "$APP_JAR" extract --layers --destination extracted

# Dynamically detect modules with jdeps and build custom JRE with jlink
RUN set -e; \
    APP_JAR=$(find extracted/application -name "*.jar" | head -n 1); \
    DETECTED_MODULES=$(jdeps \
        --multi-release 25 \
        --ignore-missing-deps \
        --print-module-deps \
        --class-path "extracted/dependencies/lib/*" \
        "$APP_JAR"); \
    ALWAYS_INCLUDE="java.logging,java.xml,jdk.crypto.ec,jdk.naming.dns"; \
    ALL_MODULES=$(echo "${DETECTED_MODULES},${ALWAYS_INCLUDE}" | tr "," "\n" | sort -u | paste -sd, -); \
    echo "Resolved modules for jlink: ${ALL_MODULES}"; \
    jlink \
        --add-modules "${ALL_MODULES}" \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-6 \
        --output /jre

# Stage 2: Minimal hardened musl runtime
FROM bellsoft/alpaquita-linux-base:stream-musl
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy custom minimal JRE and dependency libraries
COPY --from=builder /jre /opt/jre
COPY --from=builder /build/extracted/dependencies/lib/ ./lib/

# Copy the exact versioned application JAR (e.g. catalogue-loader-1.0-Final.jar)
COPY --from=builder /build/extracted/application/*.jar ./

# Create symlink so ENTRYPOINT remains static regardless of version changes
RUN ln -s /app/*.jar /app/app.jar

ENV PATH="/opt/jre/bin:${PATH}"
USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]