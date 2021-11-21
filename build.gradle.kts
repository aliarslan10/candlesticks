plugins {
  kotlin("jvm") version "1.5.31"
  application
}

application {
  mainClass.set("MainKt")
}

group = "org.example.candlesticks"
version = "1.1.1"

repositories {
  mavenCentral()
}

object DependencyVersions {
  const val coroutines = "1.5.2"
  const val http4k = "4.13.1.0"
  const val jackson = "2.13.+"
  const val mockk = "1.12.0"
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("junit:junit:4.13.1")
  testImplementation(kotlin("test"))

  implementation(platform("org.http4k:http4k-bom:4.13.1.0"))
  implementation("org.http4k:http4k-core")
  implementation("org.http4k:http4k-server-netty")
  implementation("org.http4k:http4k-client-websocket:${DependencyVersions.http4k}")
  implementation("org.http4k:http4k-format-jackson:${DependencyVersions.http4k}")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.coroutines}")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${DependencyVersions.jackson}")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${DependencyVersions.jackson}")
  testImplementation("io.mockk:mockk:${DependencyVersions.mockk}")
  testImplementation("org.junit.jupiter:junit-jupiter:5.0.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

tasks.test {
  useJUnitPlatform()
}