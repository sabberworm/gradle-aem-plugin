repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("http://dl.bintray.com/cognifide/maven-public") }
    maven { url = uri("https://dl.bintray.com/neva-dev/maven-public") }
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.60")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.10.0")
    implementation("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.10.1")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.0.1")
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    implementation("com.neva.gradle:fork-plugin:4.0.0")
    implementation("net.researchgate:gradle-release:2.8.1")
    implementation("gradle.plugin.com.github.breadmoirai:github-release:2.2.9")
}
