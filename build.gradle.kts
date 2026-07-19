plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.mutualzz"
version = "1.2.0"

java {
    // Develop on a modern JDK; emit Java 17 so Paper 1.18.2+ can load the jar.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    // Paper API metadata may request a higher JVM — keep our --release 17.
    disableAutoTargetJvm()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Compile against a recent Paper API; runtime supports 1.18.2+ via chat fallbacks.
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks {
    runServer {
        // Dev server tracks current Paper; production jar still supports 1.18.2+.
        minecraftVersion("26.1.2")
        jvmArgs("-Xms1G", "-Xmx2G")
    }
}

/** Copy a local hub config into the run server (keeps your token between restarts). */
tasks.register<Copy>("prepareDevConfig") {
    from("dev-config.yml")
    into("run/plugins/MutualzzBridge")
    rename { "config.yml" }
    onlyIf { file("dev-config.yml").exists() }
}

tasks.named("runServer") {
    dependsOn("prepareDevConfig")
}
