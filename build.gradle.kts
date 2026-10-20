plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "org.geysermc"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Web
	implementation(libs.bundles.web)

	// Database
	implementation(libs.bundles.database)
	runtimeOnly(libs.bundles.database.runtime)

	// Misc
	implementation(libs.github.api)
	implementation(libs.guava)
	compileOnly(libs.auto.service)
	annotationProcessor(libs.auto.service)

	// Testing
	testImplementation(libs.bundles.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
