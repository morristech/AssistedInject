package com.squareup.inject.assisted.dagger2.processor

import com.squareup.inject.assisted.processor.assistedInjectFactoryName
import com.squareup.inject.assisted.processor.internal.rawClassName
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

private val MODULE = ClassName.get("dagger", "Module")
private val BINDS = ClassName.get("dagger", "Binds")

data class AssistedInjectionModule(
  val moduleName: ClassName,
  val public: Boolean,
  val targetNameToFactoryName: Map<TypeName, ClassName>,
  /** An optional `@Generated` annotation marker. */
  val generatedAnnotation: AnnotationSpec? = null
) {
  val generatedType = moduleName.assistedInjectModuleName()

  fun brewJava(): TypeSpec {
    return TypeSpec.classBuilder(generatedType)
        .addAnnotation(MODULE)
        .apply {
          if (generatedAnnotation != null) {
            addAnnotation(generatedAnnotation)
          }
        }
        .addModifiers(ABSTRACT)
        .apply {
          if (public) {
            addModifiers(PUBLIC)
          }
        }
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .build())
        .applyEach(targetNameToFactoryName) { targetName, factoryName ->
          val rawTargetName = targetName.rawClassName()
          addMethod(MethodSpec.methodBuilder(rawTargetName.bindMethodName())
              .addAnnotation(BINDS)
              .addModifiers(ABSTRACT)
              .returns(factoryName)
              .addParameter(rawTargetName.assistedInjectFactoryName(), "factory")
              .build())
        }
        .build()
  }
}

private fun ClassName.bindMethodName() = "bind_" + reflectionName().replace('.', '_')

fun ClassName.assistedInjectModuleName(): ClassName = peerClass("AssistedInject_" + simpleName())

private inline fun <T : Any, K, V> T.applyEach(items: Map<K, V>, func: T.(K, V) -> Unit): T {
  items.forEach { (key, value) -> func(key, value) }
  return this
}
