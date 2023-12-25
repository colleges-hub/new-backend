# Загрузка образа для сборки приложения
FROM gradle:8.5-jdk17 AS build

# Копирование всех файлов из текущего каталога на хост-системе в Docker-образ
COPY . /home/app

# Установка рабочей директории
WORKDIR /home/app

# Запуск сборки приложения
RUN gradle clean build -x test --no-daemon

# Загрузка образа для запуска приложения
FROM eclipse-temurin:17-jre-alpine AS run

# Копирование собранного jar-файла из предыдущего образа в текущий
ARG JAR_FILE=/home/app/build/libs/*.jar
COPY --from=build ${JAR_FILE} /home/app/app.jar

# Установка рабочей директории
WORKDIR /home/app

EXPOSE 8080

# Запуск приложения
CMD ["java", "-jar", "app.jar"]
