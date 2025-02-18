package com.github.gmazzo.gradle.plugins

import com.github.gmazzo.gradle.plugins.generators.BuildConfigGenerator
import com.github.gmazzo.gradle.plugins.generators.BuildConfigGeneratorSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class BuildConfigTask : DefaultTask() {

    @get:Nested
    abstract val specs: SetProperty<BuildConfigClassSpec>

    @get:Nested
    abstract val generator: Property<BuildConfigGenerator>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generateBuildConfigFile() = outputDir.get().asFile.let { dir ->
        dir.deleteRecursively()
        dir.mkdirs()

        val generator = generator.get()

        specs.get().asSequence().filter { it.buildConfigFields.isNotEmpty() }.forEach {
            val rawClassName = it.className.get()
            val (packageName, className) = when (val rawPackage = it.packageName.orNull) {
                null -> when (val i = rawClassName.lastIndexOf('.')) {
                    -1 -> "" to rawClassName
                    else -> rawClassName.substring(0, i) to rawClassName.substring(i + 1)
                }

                else -> rawPackage to rawClassName
            }

            generator.execute(
                BuildConfigGeneratorSpec(
                    className = className,
                    packageName = packageName,
                    fields = it.buildConfigFields.sortedWith { a, b ->
                        when (val cmp = a.position.get().compareTo(b.position.get())) {
                            0 -> a.name.compareTo(b.name)
                            else -> cmp
                        }
                    },
                    outputDir = dir
                )
            )
        }
    }

}
