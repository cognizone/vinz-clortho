import org.gradle.api.JavaVersion
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.plugins.signing.SigningExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Define version variables
val commonsIoVersion = "2.11.0"
val commonsLang3Version = "3.12.0"
val httpclientVersion = "4.5.13"
val jsr305Version = "3.0.2"
val junitVersion = "5.9.2"
val assertJVersion = "3.24.2"
val mockitoVersion = "5.3.1"

plugins {
  id("pl.allegro.tech.build.axion-release") version "1.13.3"
  id("org.springframework.boot") version "3.1.5" apply false
  id("io.spring.dependency-management") version "1.1.3"
  idea
  `java`
  `java-library`
  `maven-publish`
  id("io.freefair.lombok") version "6.5.1"
}

repositories {
  mavenLocal()
  mavenCentral()
}

scmVersion {
  tag.apply {
    prefix = "v"
    versionSeparator = ""
    branchPrefix = mapOf(
            "release/.*" to "release-v",
            "hotfix/.*" to "hotfix-v"
    )
  }
  nextVersion.apply {
    suffix = "SNAPSHOT"
    separator = "-"
  }
}

idea {
  project {
    jdkName = "17"
    languageLevel = IdeaLanguageLevel("17")
  }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "idea")
  apply(plugin = "io.freefair.lombok")
  apply(plugin = "io.spring.dependency-management")
  apply(plugin = "maven-publish")
  apply(plugin = "signing")

  group = "zone.cogni.lib.vinz-clortho"
  version = rootProject.scmVersion.version

  java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
  }

  repositories {
    if (project.hasProperty("jenkins-ci")) {
      maven {
        url = uri("${System.getProperty("nexus.url")}/repository/cognizone-group")
        credentials {
          username = System.getProperty("nexus.username")
          password = System.getProperty("nexus.password")
        }
        isAllowInsecureProtocol = true
      }
    }
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.spring.io/milestone") }
  }

  dependencyManagement {
    imports {
      mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
    dependencies {
      dependency("javax.inject:javax.inject:1")
      dependency("com.google.code.findbugs:jsr305:$jsr305Version")
      dependency("commons-io:commons-io:$commonsIoVersion")
      dependency("org.apache.commons:commons-lang3:$commonsLang3Version")
      dependency("org.apache.httpcomponents:httpclient:$httpclientVersion")

      dependency("org.junit.jupiter:junit-jupiter:$junitVersion")
      dependency("org.assertj:assertj-core:$assertJVersion")
      dependency("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    }
  }

  dependencies {
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")
  }

  tasks.named<Jar>("jar") {
    from(projectDir) {
      include("LICENSE")
      into("/")
    }
    from(projectDir) {
      include("LICENSE")
      into("META-INF")
    }
  }

  tasks.named<Test>("test") {
    useJUnitPlatform()
  }

  publishing {
    publications {
      create<MavenPublication>("mavenJava") {
        groupId = project.group.toString()
        version = project.version.toString()
        from(components["java"])

        pom {
          name.set(project.name)
          packaging = "jar"
          description.set("A lightweight replacement for Zuul when using spring boot 2.7+")
          url.set("https://github.com/cognizone/vinz-clortho")

          scm {
            connection.set("scm:git:git@https://github.com/cognizone/vinz-clortho.git")
            developerConnection.set("scm:git:git@github.com:cognizone/vinz-clortho.git")
            url.set("https://github.com/cognizone/vinz-clortho")
          }

          licenses {
            license {
              name.set("The Apache License, Version 2.0")
              url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }

          developers {
            developer {
              id.set("cognizone")
              name.set("Cognizone")
              email.set("cognizone-lib-dev@cogni.zone")
            }
          }
        }
      }
    }
    repositories {
      if (project.hasProperty("publishToCognizoneNexus")) {
        maven {
          credentials {
            username = System.getProperty("nexus.username")
            password = System.getProperty("nexus.password")
          }
          val releasesRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-release"
          val snapshotsRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-snapshot"
          url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
          isAllowInsecureProtocol = true
        }
      }
      if (project.hasProperty("publishToMavenCentral")) {
        maven {
          credentials {
            username = System.getProperty("ossrh.username")
            password = System.getProperty("ossrh.password")
          }
          val stagingRepoUrl = "${System.getProperty("ossrh.url")}/service/local/staging/deploy/maven2"
          val snapshotsRepoUrl = "${System.getProperty("ossrh.url")}/content/repositories/snapshots"
          url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else stagingRepoUrl)
        }
      }
    }
  }

  // Configure the signing extension
  configure<SigningExtension> {
    if (project.hasProperty("publishToMavenCentral")) {
      sign(publishing.publications["mavenJava"])
    }
  }

  tasks.withType<Javadoc> {
    isFailOnError = false
  }
}

project(":vinz-clortho") {
  dependencies {
    implementation("javax.inject:javax.inject:1") // Required for publication build
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("com.google.code.findbugs:jsr305")
    implementation("commons-io:commons-io")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.httpcomponents:httpclient")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("javax.inject:javax.inject")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
  }
}
