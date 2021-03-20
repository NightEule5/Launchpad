plugins {
    kotlin("jvm")                  version "1.4.31"
	kotlin("plugin.serialization") version "1.4.31"
	`java-gradle-plugin`
	`maven-publish`
}

group = "strixpyrr.launchpad"
version = "0.0.1"

repositories {
    mavenCentral()
	maven(url = "https://maven.fabricmc.net/")
}

dependencies {
    implementation(kotlin("stdlib"))
	implementation(gradleKotlinDsl())
	
	compileOnly   (group = "fabric-loom",           name = "fabric-loom.gradle.plugin",  version = "0.6.9" )
	implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.1.0" )
	implementation(group = "com.squareup.okio",     name = "okio",                       version = "2.10.0")
	
	testImplementation(group = "io.kotest",         name = "kotest-runner-junit5", version = "4.4.1" )
	testImplementation(group = "io.mockk",          name = "mockk",                version = "1.10.6")
	testImplementation(group = "com.squareup.okio", name = "okio",                 version = "2.10.0")
}

sourceSets()
{
	main
	test
}

tasks()
{
	compileKotlin()
	{
		kotlinOptions.useIR             = true
		kotlinOptions.jvmTarget         = "1.8"
		kotlinOptions.languageVersion   = "1.5"
		kotlinOptions.freeCompilerArgs  =
			listOf(
				"-Xopt-in=kotlin.RequiresOptIn"
			)
	}
	
	compileTestKotlin()
	{
		kotlinOptions.useIR             = true
		kotlinOptions.jvmTarget         = "1.8"
		kotlinOptions.languageVersion   = "1.5"
		kotlinOptions.freeCompilerArgs  =
			listOf(
				"-Xopt-in=kotlin.RequiresOptIn"
			)
	}
	
	withType<Test> { useJUnitPlatform() }
}

gradlePlugin.plugins.run()
{
	val launchpad by creating
	
	launchpad.run()
	{
		id = "strixpyrr.launchpad"
		displayName = "Launchpad"
		implementationClass = "strixpyrr.launchpad.LaunchpadPlugin"
	}
}

publishing()
{
	publications()
	{
		create<MavenPublication>("Launchpad")
		{
			from(components["kotlin"])
			
			artifact(tasks.kotlinSourcesJar)
			
			repositories()
			{
				mavenLocal()
			}
		}
	}
}