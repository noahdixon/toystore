# BUILD
FROM maven:3.8.5-openjdk-17-slim AS buildstage
COPY catalog-service /home/app/catalog-service
COPY common-assets /home/app/common-assets
COPY gateway-service /home/app/gateway-service
COPY order-service /home/app/order-service
COPY client /home/app/client
COPY pom.xml /home/app/pom.xml
RUN mvn -f /home/app/pom.xml clean package

# Package now with only the required jar for the service
FROM amazoncorretto:17-alpine-jdk
COPY --from=buildstage /home/app/gateway-service/target/gatewayservice-1.0-SNAPSHOT.jar /usr/local/lib/gatewayservice.jar
EXPOSE 1764
# Cache with size 7
ENTRYPOINT ["java","-cp","/usr/local/lib/gatewayservice.jar","com.dixon.gateway.GatewayServiceServer", "-cs", "7"]

# Cache with size 0 (disabled cache)
# ENTRYPOINT ["java","-cp","/usr/local/lib/gatewayservice.jar","com.dixon.gateway.GatewayServiceServer", "-cs", "0"]