/*
 * Copyright (C) 2014 The Dagger Authors.
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
import static dagger.internal.codegen.validation.BindingElementValidator.AllowsMultibindings.ALLOWS_MULTIBINDINGS;
import static dagger.internal.codegen.validation.BindingElementValidator.AllowsScoping.NO_SCOPING;
import static dagger.internal.codegen.validation.BindingMethodValidator.Abstractness.MUST_BE_CONCRETE;
import static dagger.internal.codegen.validation.BindingMethodValidator.ExceptionSuperclass.EXCEPTION;
import static dagger.internal.codegen.xprocessing.XTypes.isTypeOf;

import androidx.room.compiler.processing.XMethodElement;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.XType;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.internal.codegen.binding.InjectionAnnotations;
import dagger.internal.codegen.xprocessing.Nullability;
import dagger.internal.codegen.xprocessing.XTypeNames;
import dagger.internal.codegen.xprocessing.XTypes;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

/** A validator for {@link dagger.producers.Produces} methods. */
final class ProducesMethodValidator extends BindingMethodValidator {

  @Inject
  ProducesMethodValidator(
      XProcessingEnv processingEnv,
      DependencyRequestValidator dependencyRequestValidator,
      InjectionAnnotations injectionAnnotations) {
    super(
        XTypeNames.PRODUCES,
        XTypeNames.PRODUCER_MODULE,
        MUST_BE_CONCRETE,
        EXCEPTION,
        ALLOWS_MULTIBINDINGS,
        NO_SCOPING,
        processingEnv,
        dependencyRequestValidator,
        injectionAnnotations);
  }

  @Override
  protected String elementsIntoSetNotASetMessage() {
    return "@Produces methods of type set values must return a Set or ListenableFuture of Set";
  }

  @Override
  protected String badTypeMessage() {
    return "@Produces methods can return only a primitive, an array, a type variable, "
        + "a declared type, or a ListenableFuture of one of those types";
  }

  @Override
  protected ElementValidator elementValidator(XMethodElement method) {
    return new Validator(method);
  }

  private class Validator extends MethodValidator {
    private final XMethodElement method;

    Validator(XMethodElement method) {
      super(method);
      this.method = method;
    }

    @Override
    protected void checkAdditionalMethodProperties() {
      checkNullable();
    }

    /**
     * Adds a warning if a {@link dagger.producers.Produces @Produces} method is declared nullable.
     */
    // TODO(beder): Properly handle nullable with producer methods.
    private void checkNullable() {
      Nullability nullability = Nullability.of(method);
      if (!nullability.nullableAnnotations().isEmpty()) {
        report.addWarning("@Nullable on @Produces methods does not do anything");
      }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Allows {@code keyType} to be a {@link ListenableFuture} of an otherwise-valid key type.
     */
    @Override
    protected void checkKeyType(XType keyType) {
      unwrapListenableFuture(keyType).ifPresent(super::checkKeyType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Allows an {@link dagger.multibindings.ElementsIntoSet @ElementsIntoSet} or {@code
     * SET_VALUES} method to return a {@link ListenableFuture} of a {@link Set} as well.
     */
    @Override
    protected void checkSetValuesType() {
      unwrapListenableFuture(method.getReturnType()).ifPresent(this::checkSetValuesType);
    }

    private Optional<XType> unwrapListenableFuture(XType type) {
      if (isTypeOf(type, XTypeNames.LISTENABLE_FUTURE)) {
        if (XTypes.isRawParameterizedType(type)) {
          report.addError("@Produces methods cannot return a raw ListenableFuture");
          return Optional.empty();
        } else {
          return Optional.of(getOnlyElement(type.getTypeArguments()));
        }
      }
      return Optional.of(type);
    }
  }
}
