/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.codegen;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor6;

/**
 * Support for annotation processors.
 */
final class CodeGen {
  private CodeGen() {
  }

  public static PackageElement getPackage(Element type) {
    while (type.getKind() != ElementKind.PACKAGE) {
      type = type.getEnclosingElement();
    }
    return (PackageElement) type;
  }

  /**
   * Returns the supertype, or {@code null} if the supertype is a platform
   * class. This is intended for annotation processors that assume platform
   * classes will never be annotated with application annotations.
   */
  public static TypeMirror getApplicationSupertype(TypeElement type) {
    TypeMirror supertype = type.getSuperclass();
    String supertypeName = supertype.toString();
    if (supertypeName.startsWith("android.")
        || supertypeName.startsWith("java.")
        || supertypeName.startsWith("javax.")) {
      return null;
    } else {
      return supertype;
    }
  }

  /** Returns a fully qualified class name to complement {@code type}. */
  public static String adapterName(TypeElement typeElement, String suffix) {
    StringBuilder builder = new StringBuilder();
    rawTypeToString(builder, typeElement, '$');
    builder.append(suffix);
    return builder.toString();
  }

  /** Returns a string like {@code java.util.List<java.lang.String>}. */
  public static String parameterizedType(Class<?> raw, String... parameters) {
    StringBuilder result = new StringBuilder();
    result.append(raw.getName());
    result.append("<");
    for (int i = 0; i < parameters.length; i++) {
      if (i != 0) {
        result.append(", ");
      }
      result.append(parameters[i]);
    }
    result.append(">");
    return result.toString();
  }

  /** Returns a string for {@code type}. Primitive types are always boxed. */
  public static String typeToString(TypeMirror type) {
    StringBuilder result = new StringBuilder();
    typeToString(type, result, '.');
    return result.toString();
  }

  /**
   * Appends a string for {@code type} to {@code result}. Primitive types are
   * always boxed.
   *
   * @param innerClassSeparator either '.' or '$', which will appear in a
   *     class name like "java.lang.Map.Entry" or "java.lang.Map$Entry".
   *     Use '.' for references to existing types in code. Use '$' to define new
   *     class names and for strings that will be used by runtime reflection.
   */
  public static void typeToString(final TypeMirror type, final StringBuilder result,
      final char innerClassSeparator) {
    type.accept(new SimpleTypeVisitor6<Void, Void>() {
      @Override public Void visitDeclared(DeclaredType declaredType, Void v) {
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        rawTypeToString(result, typeElement, innerClassSeparator);
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (!typeArguments.isEmpty()) {
          result.append("<");
          for (int i = 0; i < typeArguments.size(); i++) {
            if (i != 0) {
              result.append(", ");
            }
            typeToString(typeArguments.get(i), result, innerClassSeparator);
          }
          result.append(">");
        }
        return null;
      }
      @Override public Void visitPrimitive(PrimitiveType primitiveType, Void aVoid) {
        result.append(box((PrimitiveType) type).getName());
        return null;
      }
      @Override public Void visitArray(ArrayType arrayType, Void aVoid) {
        typeToString(arrayType.getComponentType(), result, innerClassSeparator);
        result.append("[]");
        return null;
      }
      @Override public Void visitTypeVariable(TypeVariable typeVariable, Void v) {
        return null;
      }
      @Override protected Void defaultAction(TypeMirror typeMirror, Void aVoid) {
        throw new UnsupportedOperationException("Unexpected type " + typeMirror);
      }
    }, null);
  }

  private static void rawTypeToString(StringBuilder result, TypeElement type,
      char innerClassSeparator) {
    String packageName = getPackage(type).getQualifiedName().toString();
    String qualifiedName = type.getQualifiedName().toString();
    result.append(packageName);
    result.append('.');
    result.append(
        qualifiedName.substring(packageName.length() + 1).replace('.', innerClassSeparator));
  }

  private static Class<?> box(PrimitiveType primitiveType) {
    switch (primitiveType.getKind()) {
      case BYTE:
        return Byte.class;
      case SHORT:
        return Short.class;
      case INT:
        return Integer.class;
      case LONG:
        return Long.class;
      case FLOAT:
        return Float.class;
      case DOUBLE:
        return Double.class;
      case BOOLEAN:
        return Boolean.class;
      case CHAR:
        return Character.class;
      case VOID:
        return Void.class;
      default:
        throw new AssertionError();
    }
  }
}
