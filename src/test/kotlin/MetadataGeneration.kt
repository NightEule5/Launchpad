// Copyright 2021 Strixpyrr
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package strixpyrr.launchpad.test

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import strixpyrr.launchpad.metadata.fabric.*
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class MetadataGeneration : FunSpec()
{
	private val gradle get() = GradleRunner.create()
	
	private val populatedMetadata =
		FabricMod(
			id = "test",
			version = "3.14",
			environment = Environment.Either,
			entryPoints =
				run()
				{
					val entryPoints =
						listOf(
							EntryPoint(value = "test.package.TestClass::init", adapter = "kotlin")
						)
					
					EntryPoints(
						common = entryPoints,
						server = entryPoints,
						client = entryPoints,
						additional = mapOf("custom" to entryPoints)
					)
				},
			jars = listOf(NestedJar(file = Path("Cookie.jar"))),
			languageAdapters = mapOf(/* Todo */),
			mixins = Mixins(config = Path("mixin.json")),
			depends = mapOf("Dependency" to ">=7.13"),
			recommends = mapOf("Recommended" to ">=7.13"),
			suggests = mapOf("Suggests" to ">=7.13"),
			conflicts = mapOf("Conflicts" to ">=7.13"),
			breaks = mapOf("Breaks" to ">=7.13"),
			name = "Totally descriptive test name.",
			description = "Totally descriptive... description.",
			authors =
				listOf(
					Person(
						"Anonymous",
						ContactInfo(
							additional = mapOf("discord" to "Anonymous#----")
						)
					)
				),
			contributors =
			listOf(
				Person(
					"TotallyNotAnonymous",
					ContactInfo(
						additional = mapOf("discord" to "TotallyNotAnonymous#----")
					)
				)
			),
			contact =
				ContactInfo(
					email = "convincing@email.com",
					issues = "https://github.com/Anonymous/Test/issues",
					sources = "https://github.com/Anonymous/Test"
				),
			license = listOf("All rights reserved"),
			icon = Icon(path = Path.of("icon.png")),
			//custom = mapOf("Boop" to "Ow, that hurt!")
		)
	
	init
	{
		context("Metadata")
		{
			val root = Path("testbed")
			
			context("Generate Populated Metadata")
			{
				test("Fabric")
				{
					val testDirectory = "Generate Populated Metadata/Fabric"
					
					generateBuild(root, testDirectory)
					
					val fabricModJson = root / "build" / "generated-sources" / "fabric-mod-metadata" / "fabric.mod.json"
					
					val build = gradle.withGradleVersion("6.8.3")
									   .withProjectDir(root.toFile())
									   .withPluginClasspath()
									   .withArguments("generateFabricMetadata")
									   .forwardOutput()
									   .build()
					
					val outcome = build.task("generateFabricMetadata")?.outcome
					
					if (outcome != null)
						outcome shouldBe TaskOutcome.SUCCESS
					
					fabricModJson.shouldExist()
				}
			}
		}
	}
	
	private fun generateBuild(root: Path, testDirectory: String)
	{
		if (root.notExists()) root.createDirectory()
		
		resource(path = "/$testDirectory/build.gradle.kts") copyIfNewerTo root / "build.gradle.kts"
		
		resource(path = "/$testDirectory/settings.gradle.kts") copyIfNewerTo root / "settings.gradle.kts"
	}
}