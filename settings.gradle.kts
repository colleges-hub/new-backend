rootProject.name = "backend"

gradle.allprojects {
    repositories {
        mavenCentral()
    }
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}