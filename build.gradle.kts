plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.mutualzz"
version = "1.0.2"

java {
    // Paper 26.1.x requires JDK 25
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("com.google.code.gson:gson:2.11.0")
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
        // Downloads Paper, builds this plugin, drops the jar into plugins/, starts the server
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
