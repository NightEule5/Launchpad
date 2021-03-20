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

import okio.*
import okio.ByteString.Companion.encodeUtf8
import strixpyrr.launchpad.internal.using
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.reflect.KClass

@Suppress("unused") // Used for type inference.
internal inline fun <reified T : Any> T.resource(path: String) = readResource(owner = T::class, path)

internal fun readResource(owner: KClass<out Any>, path: String) =
	owner.java.getResourceAsStream(path).source().buffer()

internal infix fun BufferedSource.copyTo(sink: Sink) =
	using(this)
	{ source ->
		using(sink) { sink -> source.readAll(sink) }
	}

internal infix fun BufferedSource.copyIfNewerTo(to: Path) = use()
{
	// Read to a buffer.
	
	val buffer = Buffer()
	
	buffer.writeAll(source = this)
	
	buffer
}.copyIfNewerTo(to)

@OptIn(ExperimentalPathApi::class)
internal infix fun Buffer.copyIfNewerTo(to: Path) = use()
{ source ->
	if (to.exists())
	{
		// Read the existing file to a buffer.
		val existing = Buffer()
		
		existing.writeAll(source = to.source())
		
		// Compare hashes
		if (source isEquivalentTo existing) return@use false
	}
	
	to.sink(CREATE, TRUNCATE_EXISTING).use(source::readAll)
	
	true
}

private infix fun Buffer.isEquivalentTo(other: Buffer) =
	other.use { sha1() == it.sha1() }

@Suppress("NOTHING_TO_INLINE")
internal inline infix fun BufferedSource.replacing(replacement: Pair<String, Buffer>) =
	replaceOnce(replacement.first, replacement.second)

internal fun BufferedSource.replaceOnce(key: String, value: Buffer) = use()
{ source ->
	val keyBytes = "\$$key".encodeUtf8()
	val length = keyBytes.size.toLong()
	
	val i = source.indexOf(keyBytes)
	
	Buffer().use()
	{ intermediary ->
		if (i > -1)
		{
			// Write the source to the intermediary, swapping the replacement
			// for the actual value.
			
			intermediary.write(source, byteCount = i)
			
			value.use()
			{
				intermediary.write(source = it, byteCount = Long.MAX_VALUE)
			}
			
			source.skip(byteCount = length)
			
			intermediary.write(source, byteCount = Long.MAX_VALUE)
		}
		
		intermediary
	}
}