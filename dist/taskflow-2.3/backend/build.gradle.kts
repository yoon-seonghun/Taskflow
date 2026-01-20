plugins {
    java
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.taskflow"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // MyBatis (JPA 사용 금지)
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3")

    // Database - Primary (MySQL)
    runtimeOnly("com.mysql:mysql-connector-j")

    // Database - External DB Support (Oracle, MSSQL, Tibero)
    runtimeOnly("com.oracle.database.jdbc:ojdbc11:21.9.0.0")
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre11")
    runtimeOnly(files("libs/tibero6-jdbc.jar"))

    // Caching (Caffeine)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // HTML Parser (for plain text extraction)
    implementation("org.jsoup:jsoup:1.17.2")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // SFTP (JSch maintained fork)
    implementation("com.github.mwiede:jsch:0.2.18")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
