/*
 * Copyright (C) 2016 The Dagger Authors.
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

package dagger.internal.codegen.validation;

import static com.google.common.collect.Iterables.getOnlyElement;
import static dagger.internal.codegen.base.Util.reentrantComputeIfAbsent;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableSet;
import static dagger.internal.codegen.langmodel.DaggerElements.isAnnotationPresent;
import static dagger.internal.codegen.langmodel.DaggerElements.isAnyAnnotationPresent;
import static java.util.stream.Collectors.joining;

import androidx.room.compiler.processing.XExecutableElement;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.compat.XConverters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import dagger.internal.codegen.base.ClearableCache;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.ExecutableElement;

/** Validates any binding method. */
@Singleton
public final class AnyBindingMethodValidator implements ClearableCache {
  private final ImmutableMap<ClassName, BindingMethodValidator> validators;
  private final Map<ExecutableElement, ValidationReport> reports = new HashMap<>();
  private final XProcessingEnv processingEnv;

  @Inject
  AnyBindingMethodValidator(
      ImmutableMap<ClassName, BindingMethodValidator> validators, XProcessingEnv processingEnv) {
    this.validators = validators;
    this.processingEnv = processingEnv;
  }

  @Override
  public void clearCache() {
    reports.clear();
  }

  /** Returns the binding method annotations considered by this validator. */
  ImmutableSet<ClassName> methodAnnotations() {
    return validators.keySet();
  }

  /**
   * Returns {@code true} if {@code method} is annotated with at least one of {@link
   * #methodAnnotations()}.
   */
  boolean isBindingMethod(XExecutableElement method) {
    return isAnyAnnotationPresent(XConverters.toJavac(method), methodAnnotations());
  }

  /**
   * Returns a validation report for a method.
   *
   * <ul>
   *   <li>Reports an error if {@code method} is annotated with more than one {@linkplain
   *       #methodAnnotations() binding method annotation}.
   *   <li>Validates {@code method} with the {@link BindingMethodValidator} for the single
   *       {@linkplain #methodAnnotations() binding method annotation}.
   * </ul>
   *
   * @throws IllegalArgumentException if {@code method} is not annotated by any {@linkplain
   *     #methodAnnotations() binding method annotation}
   */
  ValidationReport validate(ExecutableElement method) {
    return reentrantComputeIfAbsent(reports, method, this::validateUncached);
  }

  /**
   * Returns {@code true} if {@code method} was already {@linkplain #validate(ExecutableElement)
   * validated}.
   */
  boolean wasAlreadyValidated(XExecutableElement method) {
    return reports.containsKey(XConverters.toJavac(method));
  }

  private ValidationReport validateUncached(ExecutableElement method) {
    ValidationReport.Builder report = ValidationReport.about(method);
    ImmutableSet<ClassName> bindingMethodAnnotations =
        methodAnnotations().stream()
            .filter(annotation -> isAnnotationPresent(method, annotation))
            .collect(toImmutableSet());
    switch (bindingMethodAnnotations.size()) {
      case 0:
        throw new IllegalArgumentException(
            String.format("%s has no binding method annotation", method));

      case 1:
        report.addSubreport(
            validators
                .get(getOnlyElement(bindingMethodAnnotations))
                .validate(XConverters.toXProcessing(method, processingEnv)));
        break;

      default:
        report.addError(
            String.format(
                "%s is annotated with more than one of (%s)",
                method.getSimpleName(),
                methodAnnotations().stream().map(ClassName::canonicalName).collect(joining(", "))),
            method);
        break;
    }
    return report.build();
  }
}
