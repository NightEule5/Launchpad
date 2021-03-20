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
package strixpyrr.launchpad.metadata.fabric

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.buffer
import okio.sink
import okio.source
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import javax.inject.Inject
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
open class GenerateFabricModMetadataTask(@get:OutputFile val output: Path) : DefaultTask()
{
	@Inject constructor(project: Project) : this(project, project.mainSourceSet)
	constructor(project: Project, sourceSet: SourceSet) :
		this(project.buildDir / "generated-sources" / "fabric-mod-metadata" / "fabric.mod.json")
	{
		val generated = output.parent
		
		sourceSet.resources.srcDir(generated)
		
		// Generate the metadata before resources are processed.
		@Suppress("UnstableApiUsage")
		project.tasks.named(
			sourceSet.processResourcesTaskName,
			ProcessResources::class.java
		)
		{
			dependsOn(this)
		}
	}
	
	lateinit var metadata: FabricMod
	
	var produceHumanReadableOutput = false
	
	init
	{
		outputs.upToDateWhen { isNotCurrent }
	}
	
	@TaskAction
	fun generate()
	{
		didWork = true
		
		if (!::metadata.isInitialized)
		{
			output.deleteExisting()
			
			return
		}
		
		output.parent.run { if (notExists()) createDirectory() }
		
		output.sink(CREATE, TRUNCATE_EXISTING)
			  .buffer()
			  .use()
			  { sink ->
				  metadata.encode(
					  sink,
					  Json()
					  {
						  prettyPrint = produceHumanReadableOutput
						  prettyPrintIndent = "\t"
						  encodeDefaults = false // Here for clarity.
					  }
				  )
			  }
	}
	
	@get:[JvmSynthetic JvmName("isNotCurrent")]
	internal val isNotCurrent: Boolean get()
	{
		val output = output
		
		if (output.exists())
		{
			if (!::metadata.isInitialized)
				return true
			
			return try
			{
				val existing =
					FabricMod.decode(
						source = output.source().buffer()
					)
				
				metadata != existing
			}
			catch (_: SerializationException)
			{
				true
			}
		}
		else
		{
			if (!::metadata.isInitialized)
				return false
			
			return true
		}
	}
	
	companion object
	{
		private val Project.mainSourceSet get() =
			extensions.getByName<SourceSetContainer>("sourceSets")["main"]
		
		private operator fun File.div(value: String) = toPath() / value
	}
}