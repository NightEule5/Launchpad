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
package strixpyrr.launchpad

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import strixpyrr.launchpad.metadata.fabric.GenerateFabricModMetadataTask

class LaunchpadPlugin : Plugin<Project>
{
	override fun apply(target: Project)
	{
		target.tasks.register(
			"generateFabricMetadata",
			GenerateFabricModMetadataTask::class,
			target
		)
	}
}