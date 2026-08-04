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

# 2. Étape d'exécution : JRE seul, exécuté par un utilisateur non privilégié
FROM eclipse-temurin:21-jre-alpine

# Image Alpine -> outils busybox (addgroup / adduser)
# -S : compte/groupe "système" (pas de mot de passe, pas d'expiration)
# -D : pas de mot de passe, -H : pas de création de home
RUN addgroup -g 1001 -S appgroup \
    && adduser -S -D -H -u 1001 -G appgroup appuser

# Port applicatif > 1024 : bindable par un utilisateur non root
EXPOSE 8081

WORKDIR /app

# Copier le JAR exécutable (le résultat de l'étape 'builder')
# Le nom par défaut du JAR Spring Boot est généralement <nom-projet>-<version>.jar
# Adaptez le nom si nécessaire, ici nous utilisons un wildcard (*)
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar /app/app.jar

# Propriété et droits sur le répertoire de travail de l'application
RUN chown -R appuser:appgroup /app

# Bascule sur l'utilisateur non privilégié (uid/gid 1001)
USER appuser

# Commande pour démarrer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]