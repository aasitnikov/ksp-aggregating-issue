package com.example.experiments.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName

class ExperimentsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    // https://github.com/google/ksp/blob/main/docs/multi-round.md
    var alreadyProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("start to reprocess")
        val symbols = resolver.getSymbolsWithAnnotation("com.example.experiments.annotation.ExperimentAnnotation")
        logger.warn("symbols = ${symbols.map { (it.location as FileLocation).filePath.substringAfterLast("/") }.toList()}")
        if (symbols.iterator().hasNext() && alreadyProcessed) throw Exception("created new annotated classes in generated code")
        val packageName = requireNotNull(options[OPTION_PACKAGE_NAME]) {
            """
                No option 'experiments_provider_package_name' provided. Please add package name option like this: 
                  ksp { arg("experiments_provider_package_name", "com.example.kek") }
            """.trimIndent()
        }
        val prefix = options[OPTION_PREFIX] ?: ""

        val classDeclarations = symbols
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()

        val annotatedClasses = processAnnotatedClasses(classDeclarations).toList()

        if (annotatedClasses.isNotEmpty()) {
            val sourceFiles = classDeclarations.mapNotNull { it.containingFile }.distinct().toList().toTypedArray()
            logger.warn("sourceFiles = ${sourceFiles.map { it.fileName }}")

            val file = codeGenerator.createNewFile(
                Dependencies(aggregating = true, sources = sourceFiles),
                packageName = packageName,
                fileName = "${prefix}ExperimentsProvider"
            )

            val generatedFile = ExperimentsCodeGenerator.generateCode(packageName, prefix, annotatedClasses)
            file.bufferedWriter().use { generatedFile.writeTo(it) }
            alreadyProcessed = true
        }

        return symbols.filterNot { it.validate() }.toList()
    }

    private fun processAnnotatedClasses(classDeclarations: Sequence<KSClassDeclaration>): Sequence<ExperimentClassInfo> {
        return classDeclarations.map { classDeclaration ->
            ExperimentClassInfo(classDeclaration.packageName.asString(), classDeclaration.simpleName.asString())
        }
    }
}

internal val BASE_EXPERIMENT_CLASS_NAME = ClassName("com.example.experiments", "Experiment")
internal const val OPTION_PACKAGE_NAME = "experiments_provider_package_name"
internal const val OPTION_PREFIX = "experiments_provider_prefix"