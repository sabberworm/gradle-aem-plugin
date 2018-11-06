import com.cognifide.gradle.aem.instance.SatisfyTask
import com.cognifide.gradle.aem.pkg.ComposeTask

plugins {
    id("com.cognifide.aem.package")
    id("com.cognifide.aem.instance")
    kotlin("jvm") apply false
}

description = "Example"
defaultTasks = listOf(":aemSatisfy", ":aemDeploy")

allprojects {
    val subproject = this@allprojects

    group = "com.company.aem"
    version = "1.0.0-SNAPSHOT"

    repositories {
        maven { url = uri("https://repo.adobe.com/nexus/content/groups/public") }
        maven { url = uri("https://repo1.maven.org/maven2") }
        jcenter()
        mavenLocal()
    }

    plugins.withId("com.cognifide.aem.base") {
        aem {
            config {
                localInstance("http://localhost:4502")
                localInstance("http://localhost:4503")
            }
        }
    }

    plugins.withId("com.cognifide.aem.bundle") {
        aem {
            bundle {
                attribute("Bundle-Category", "example")
                attribute("Bundle-Vendor", "Company")
            }
        }

        subproject.dependencies {
            "compileOnly"("org.slf4j:slf4j-api:1.5.10")
            "compileOnly"("org.osgi:osgi.cmpn:6.0.0")
        }
    }
}

tasks.named<SatisfyTask>("aemSatisfy") {
    packages {
        group("dependencies") {
            // local("pkg/vanityurls-components-1.0.2.zip")
        }

        group("tools") {
            url("https://github.com/OlsonDigital/aem-groovy-console/releases/download/9.0.1/aem-groovy-console-9.0.1.zip")
            url("https://github.com/Cognifide/APM/releases/download/cqsm-2.0.0/apm-2.0.0.zip")
        }
    }
}

tasks.named<ComposeTask>("aemCompose") {
    fromProject(":common")
    fromProject(":core")
    fromProject(":design")
}