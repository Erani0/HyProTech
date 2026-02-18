plugins {
    id("java")
}

group = "HyProTechTeam"
//version = "1.4.1-SNAPSHOT"
version = "1.3.3"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

sourceSets {
    main {
        java.srcDirs("src/main/java")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://www.cursemaven.com") } // Main repo
    maven { url = uri("https://jitpack.io") } // Can be useful to compatibility
}
val hytalePath = File(System.getProperty("user.home"), "AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar")

dependencies {
    //compileOnly(files("libs/HytaleServer.jar"))
    compileOnly(files(hytalePath))
    compileOnly(fileTree("libs") {
        include("**/TuTBooKs*.jar", "**/TutBooks*.jar")
    })
    compileOnly("curse.maven:hyui-1431415:7548594")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}