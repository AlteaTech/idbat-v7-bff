# 1. Étape de construction : Utiliser une image avec le JDK et Gradle
# Utilisation d'une image basée sur Debian/Ubuntu (eclipse-temurin) au lieu d'Alpine pour éviter les problèmes de bibliothèques natives (glibc vs musl)
FROM eclipse-temurin:21-jdk AS builder

# Définir le répertoire de travail dans le conteneur
WORKDIR /app

# Copier les fichiers de configuration Gradle
COPY build.gradle.kts settings.gradle.kts /app/

# Copier le wrapper Gradle
COPY gradlew /app/
# Copier le dossier gradle/ qui contient le wrapper (gradle-wrapper.jar et properties)
COPY gradle/ /app/gradle/

# Copier le code source
COPY src /app/src

RUN chmod +x gradlew

RUN ./gradlew bootJar --no-daemon -x test
FROM eclipse-temurin:21-jre-alpine

EXPOSE 8081

WORKDIR /app

# Copier le JAR exécutable (le résultat de l'étape 'builder')
# Le nom par défaut du JAR Spring Boot est généralement <nom-projet>-<version>.jar
# Adaptez le nom si nécessaire, ici nous utilisons un wildcard (*)
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# Commande pour démarrer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]