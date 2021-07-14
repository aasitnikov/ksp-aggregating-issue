package com.example.experiments.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

object ExperimentsCodeGenerator {

    fun generateCode(
        packageName: String,
        providerPrefix: String,
        annotatedClasses: List<ExperimentClassInfo>
    ): FileSpec {
        val buildListBlock = CodeBlock.builder()
            .beginControlFlow("buildList")
            .apply {
                annotatedClasses.sortedBy { it.simpleName }.forEach { annotatedClass ->
                    addStatement(
                        "add(%T())",
                        ClassName(annotatedClass.packageName, annotatedClass.simpleName)
                    )
                }
            }
            .endControlFlow()
            .build()
        return FileSpec.builder(packageName, "${providerPrefix}ExperimentsProvider")
            .addType(
                TypeSpec.objectBuilder("${providerPrefix}ExperimentsProvider")
                    .addProperty(
                        PropertySpec.builder("experiments", List::class.asClassName().parameterizedBy(
                            BASE_EXPERIMENT_CLASS_NAME
                        ))
                            .addAnnotation(
                                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                                    .addMember("ExperimentalStdlibApi::class")
                                    .build()
                            )
                            .initializer(buildListBlock)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}