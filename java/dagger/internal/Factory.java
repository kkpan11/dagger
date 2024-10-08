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

package dagger.internal;

import dagger.Provides;
import javax.inject.Inject;
import javax.inject.Scope;
import org.jspecify.annotations.Nullable;

/**
 * An {@linkplain Scope unscoped} {@link Provider}. While a {@link Provider} <i>may</i> apply
 * scoping semantics while providing an instance, a factory implementation is guaranteed to exercise
 * the binding logic ({@link Inject} constructors, {@link Provides} methods) upon each call to
 * {@link #get}.
 *
 * <p>Note that while subsequent calls to {@link #get} will create new instances for bindings such
 * as those created by {@link Inject} constructors, a new instance is not guaranteed by all
 * bindings. For example, {@link Provides} methods may be implemented in ways that return the same
 * instance for each call.
 */
public interface Factory<T extends @Nullable Object> extends Provider<T> {}
