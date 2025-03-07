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

package dagger.internal;

import static dagger.internal.Preconditions.checkNotNull;
import static dagger.internal.Providers.asDaggerProvider;

import dagger.Lazy;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Lazy} and {@link Provider} implementation that memoizes the value returned from a
 * delegate using the double-check idiom described in Item 71 of <i>Effective Java 2</i>.
 */
public final class DoubleCheck<T extends @Nullable Object> implements Provider<T>, Lazy<T> {
  private static final Object UNINITIALIZED = new Object();

  private volatile @Nullable Provider<T> provider;
  private volatile @Nullable Object instance = UNINITIALIZED;

  private DoubleCheck(Provider<T> provider) {
    assert provider != null;
    this.provider = provider;
  }

  @SuppressWarnings("unchecked") // cast only happens when result comes from the provider
  @Override
  public T get() {
    @Nullable Object result = instance;
    if (result == UNINITIALIZED) {
      result = getSynchronized();
    }
    return (T) result;
  }

  @SuppressWarnings("nullness:dereference.of.nullable") // provider is non-null
  private synchronized @Nullable Object getSynchronized() {
    @Nullable Object result = instance;
    if (result == UNINITIALIZED) {
      result = provider.get();
      instance = reentrantCheck(instance, result);
      /* Null out the reference to the provider. We are never going to need it again, so we
       * can make it eligible for GC. */
      provider = null;
    }
    return result;
  }

  /**
   * Checks to see if creating the new instance has resulted in a recursive call. If it has, and the
   * new instance is the same as the current instance, return the instance. However, if the new
   * instance differs from the current instance, an {@link IllegalStateException} is thrown.
   */
  private static @Nullable Object reentrantCheck(
      @Nullable Object currentInstance, @Nullable Object newInstance) {
    boolean isReentrant = currentInstance != UNINITIALIZED;
    if (isReentrant && currentInstance != newInstance) {
      throw new IllegalStateException("Scoped provider was invoked recursively returning "
          + "different results: " + currentInstance + " & " + newInstance + ". This is likely "
          + "due to a circular dependency.");
    }
    return newInstance;
  }

  /** Returns a {@link Provider} that caches the value from the given delegate provider. */
  public static <T extends @Nullable Object> dagger.internal.Provider<T> provider(
      dagger.internal.Provider<T> delegate) {
    checkNotNull(delegate);
    if (delegate instanceof DoubleCheck) {
      /* This should be a rare case, but if we have a scoped @Binds that delegates to a scoped
       * binding, we shouldn't cache the value again. */
      return delegate;
    }
    return new DoubleCheck<T>(delegate);
  }

  /**
   * Legacy javax version of the method to support libraries compiled with an older version of
   * Dagger. Do not use directly.
   */
  @Deprecated
  public static <P extends javax.inject.Provider<T>, T> javax.inject.Provider<T> provider(
      P delegate) {
    return provider(asDaggerProvider(delegate));
  }

  /** Returns a {@link Lazy} that caches the value from the given provider. */
  public static <T extends @Nullable Object> Lazy<T> lazy(Provider<T> provider) {
    if (provider instanceof Lazy) {
      @SuppressWarnings("unchecked")
      final Lazy<T> lazy = (Lazy<T>) provider;
      // Avoids memoizing a value that is already memoized.
      // NOTE: There is a pathological case where Provider<P> may implement Lazy<L>, but P and L
      // are different types using covariant return on get(). Right now this is used with
      // DoubleCheck<T> exclusively, which is implemented such that P and L are always
      // the same, so it will be fine for that case.
      return lazy;
    }
    return new DoubleCheck<T>(checkNotNull(provider));
  }

  /**
   * Legacy javax version of the method to support libraries compiled with an older version of
   * Dagger. Do not use directly.
   */
  public static <P extends javax.inject.Provider<T>, T> Lazy<T> lazy(P provider) {
    return lazy(asDaggerProvider(provider));
  }
}
