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
@file:UseSerializers(PathSerializer::class)
package strixpyrr.launchpad.metadata.fabric

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import okio.BufferedSink
import okio.BufferedSource
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path

fun FabricMod.encode(sink: BufferedSink, json: Json = Json.Default) =
	sink.use { sink.writeUtf8(string = json.encodeToString(value = this)) }

fun FabricMod.Companion.decode(source: BufferedSource, json: Json = Json.Default) =
	source.use { json.decodeFromString<FabricMod>(string = source.readUtf8()) }

// Mostly implements the schema: http://json.schemastore.org/fabric.mod.json
// There are a few features omited though, namely most types labeled as "oneOf"
// (with the exception of Icon).
@Suppress("SpellCheckingInspection")
@Serializable
data class FabricMod(
	val id: String,
	val version: String,
	val schemaVersion: Int = 1,
	val environment: Environment? = Environment.Either,
	@SerialName("entrypoints")
	@Serializable(with = EntryPointsSerializer::class)
	val entryPoints: EntryPoints? = null,
	val jars: List<NestedJar>? = null,
	val languageAdapters: Map<String, String>? = null,
	val mixins: Mixins? = null,
	val depends: Map<String, String>? = null,
	val recommends: Map<String, String>? = null,
	val suggests: Map<String, String>? = null,
	val conflicts: Map<String, String>? = null,
	val breaks: Map<String, String>? = null,
	val name: String? = null,
	val description: String? = null,
	val authors: List<Person>? = null,
	val contributors: List<Person>? = null,
	val contact: ContactInfo? = null,
	val license: List<String>? = null,
	@Serializable(with = IconSerializer::class)
	val icon: Icon? = null,
	// Todo: Any is not serializable. We'll have to figure out another way.
	// val custom: Map<String, Any?>? = null
)
{
	init
	{
		require(id matches IdRegex) { "The mod identifier is invalid." }
	}
	
	companion object
	{
		private val IdRegex = Regex("^[a-z][a-z0-9-_]{1,63}$")
	}
}

@Serializable
enum class Environment
{
	@SerialName("*"     ) Either,
	@SerialName("client") Client,
	@SerialName("server") Server
}

@Serializable
data class EntryPoint(
	val value: String,
	val adapter: String = "default"
)
{
	init
	{
		if (value.isEmpty())
			throw Exception("The entry point value cannot be empty.")
	}
}

@Serializable
data class EntryPoints(
	@SerialName("main")
	val common: List<EntryPoint>? = null,
	val server: List<EntryPoint>? = null,
	val client: List<EntryPoint>? = null,
	val additional: Map<String, List<EntryPoint>>? = null
)
{
	companion object
	{
		internal val knownKeys get() =
			arrayOf("common", "server", "client")
	}
}

@Serializable
data class NestedJar(
	val file: Path
)

@Serializable
data class Mixins(
	val config: Path,
	val environment: Environment = Environment.Either
)

@Serializable
data class ContactInfo(
	val email   : String? = null,
	val irc     : String? = null,
	val homepage: String? = null,
	val issues  : String? = null,
	val sources : String? = null,
	val additional: Map<String, String>? = null
)
{
	companion object
	{
		internal val knownKeys get() =
			arrayOf("email", "irc", "homepage", "issues", "sources")
	}
}

@Serializable
data class Person(
	val name: String,
	@Serializable(with = ContactInfoSerializer::class)
	val contact: ContactInfo? = null
)

@Serializable
data class Icon(
	val path: Path? = null,
	val paths: Map<String, Path>? = null
)
{
	init
	{
		require(path != null || paths != null)
		{
			"Either Path or Paths must have a value."
		}
		
		if (paths != null)
			for (key in paths.keys)
				require(key matches KeyRegex)
				{
					"An icon key must be the icon's width: [$key] is not a valid key."
				}
	}
	
	companion object
	{
		private val KeyRegex = Regex("^[1-9][0-9]*$")
	}
}

// Serialization Sorcery

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Path::class)
internal object PathSerializer : KSerializer<Path>
{
	override val descriptor get() = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)
	
	override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString("$value")
	
	@OptIn(ExperimentalPathApi::class)
	override fun deserialize(decoder: Decoder) = Path(decoder.decodeString())
}

// Todo: Is there a more efficient way to do this?

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = EntryPoints::class)
internal object EntryPointsSerializer : AdditionalPropertySerializer<EntryPoints>(EntryPoints.serializer(), EntryPoints.knownKeys)
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = ContactInfo::class)
internal object ContactInfoSerializer : AdditionalPropertySerializer<ContactInfo>(ContactInfo.serializer(), ContactInfo.knownKeys)

internal open class AdditionalPropertySerializer<T : Any>(
	parent: KSerializer<T>, private val knownKeys: Array<String>
) : JsonTransformingSerializer<T>(parent)
{
	override fun transformSerialize(element: JsonElement): JsonElement
	{
		if (element !is JsonObject || "additional" !in element) return element
		
		val additional = element["additional"] as JsonObject
		
		if (additional.isEmpty()) return element
		
		val elements = (element + additional) as LinkedHashMap
		
		elements -= "additional"
		
		return JsonObject(elements)
	}
	
	override fun transformDeserialize(element: JsonElement): JsonElement
	{
		if (element !is JsonObject || element.keys in knownKeys) return element
		
		val additional = LinkedHashMap<String, JsonElement>(element.size)
		val known      = LinkedHashMap<String, JsonElement>(element.size)
		
		element.filterTo(known)
		{ (key, value) ->
			if (key in knownKeys)
				true
			else
			{
				additional[key] = value
				
				false
			}
		}
		
		known["additional"] = JsonObject(additional)
		
		return JsonObject(known)
	}
	
	companion object
	{
		private operator fun <T> Array<T>.contains(subset: Set<T>) =
			subset.all { it in this }
	}
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Icon::class)
internal object IconSerializer : OneOfSerializer<Icon>(Icon.serializer())
{
	override fun transformDeserialize(element: JsonElement) =
		JsonObject(
			mapOf(
				when (element)
				{
					is JsonPrimitive ->
					{
						require(element.isString) { "Unknown type." }
						
						"path" to element
					}
					is JsonObject    -> "paths" to element
					else             -> error("Unknown type.")
				}
			)
		)
}

internal abstract class OneOfSerializer<T : Any>(
	parent: KSerializer<T>
) : JsonTransformingSerializer<T>(parent)
{
	override fun transformSerialize(element: JsonElement): JsonElement
	{
		check(element is JsonObject)
		
		for (value in element.values)
			if (value !== JsonNull)
				return value
		
		error("No values")
	}
}