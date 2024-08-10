plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testApi(kotlin("test"))
}