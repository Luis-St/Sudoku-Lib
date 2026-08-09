val mavenUserName: String? = System.getenv("MAVEN_USERNAME")
val mavenPassword: String? = System.getenv("MAVEN_PASSWORD")
val projectVersion: String? = System.getenv("VERSION")?.replace("v", "", ignoreCase = true)

plugins {
	`java-library`
	`maven-publish`
}

repositories {
	mavenCentral()
}

dependencies {
	// Nullability
	api(libs.jspecify)

	// Testing
	testImplementation(platform("org.junit:junit-bom:6.0.0"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}

	withSourcesJar()
	withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(17)
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			if (projectVersion != null) {
				groupId = "net.luis"
				artifactId = "sudoku-lib"
				version = projectVersion
				artifact(tasks.named<Jar>("jar"))
				artifact(tasks.named<Jar>("sourcesJar"))
				artifact(tasks.named<Jar>("javadocJar"))
			} else {
				System.err.println("No version provided. Set the VERSION environment variable.")
			}
		}
	}
	repositories {
		if (mavenUserName != null && mavenPassword != null) {
			maven {
				url = uri("https://maven.luis-st.net/libraries/")
				credentials {
					username = mavenUserName
					password = mavenPassword
				}
			}
		} else {
			System.err.println("No credentials provided. Publishing to maven.luis-st.net not possible.")
		}
	}
}

tasks.named<Javadoc>("javadoc") {
	options {
		memberLevel = JavadocMemberLevel.PRIVATE
		(this as StandardJavadocDocletOptions).addStringOption("tag", "apiNote:a:API Note:")
	}
}

tasks.named<Jar>("jar") {
	manifest {
		attributes(
			"Built-By" to "Luis Staudt",
			"Multi-Release" to "true",
			"Implementation-Title" to rootProject.name,
			"Implementation-Version" to (projectVersion ?: "0.0.0"),
			"Implementation-Vendor" to "Luis Staudt",
			"Implementation-URL" to "https://www.luis-st.net/",
		)
	}
}

// The generation bench is a measurement tool, not a test: it is run on demand and prints a table, so it stays out
// of the JUnit suite and off the CI path. See net.luis.sudoku.bench.GenerationBench.
tasks.register<JavaExec>("bench") {
	group = "verification"
	description = "Measures generator band hit rate, timing and hardest-technique distribution."
	classpath = sourceSets["test"].runtimeClasspath
	mainClass.set("net.luis.sudoku.bench.GenerationBench")
	listOf("seeds", "size", "variant").forEach { name ->
		if (project.hasProperty(name)) {
			systemProperty(name, project.property(name)!!)
		}
	}
}

// One-off runner for throwaway probes against the test classpath: ./gradlew probe -Pmain=<fqcn>
tasks.register<JavaExec>("probe") {
	group = "verification"
	classpath = sourceSets["test"].runtimeClasspath
	mainClass.set(project.findProperty("main")?.toString() ?: "net.luis.sudoku.bench.GenerationBench")
}

tasks.test {
	// Lets the strategy soundness sweep be widened on demand: ./gradlew test -Psoundness.seeds=12
	project.findProperty("soundness.seeds")?.let { systemProperty("soundness.seeds", it) }
}
