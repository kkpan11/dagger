/*
 * Copyright (C) 2023 The Dagger Authors.
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

package dagger.functional.kotlinsrc.assisted

import com.google.common.truth.Truth.assertThat
import dagger.Component
import dagger.assisted.AssistedFactory
import dagger.functional.kotlinsrc.assisted.subpackage.AccessibleFoo
import dagger.functional.kotlinsrc.assisted.subpackage.AssistedDep
import dagger.functional.kotlinsrc.assisted.subpackage.InaccessibleFooFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class AssistedFactoryInaccessibleTest {
  @Component
  interface ParentComponent {
    // Factory for an accessible type from another package
    fun accessibleFooFactory(): AccessibleFooFactory

    // Factory for an inaccessible type from another package
    fun inaccessibleFooFactory(): InaccessibleFooFactory
  }

  @AssistedFactory
  interface AccessibleFooFactory {
    // Use different parameter names than Foo to make sure we're not assuming they're the same.
    fun create(factoryAssistedDep: AssistedDep): AccessibleFoo
  }

  @Test
  fun testAccessibleFooFactory() {
    val assistedDep = AssistedDep()
    val accessibleFoo =
      DaggerAssistedFactoryInaccessibleTest_ParentComponent.create()
        .accessibleFooFactory()
        .create(assistedDep)
    assertThat(accessibleFoo).isNotNull()
    assertThat(accessibleFoo.dep).isNotNull()
    assertThat(accessibleFoo.assistedDep).isEqualTo(assistedDep)
  }

  @Test
  fun testInaccessibleFooFactory() {
    val assistedDep = AssistedDep()
    // We can't access InaccessibleFoo directly, so just use Object instead.
    val inaccessibleFoo =
      DaggerAssistedFactoryInaccessibleTest_ParentComponent.create()
        .inaccessibleFooFactory()
        .create(assistedDep)
    assertThat(inaccessibleFoo).isNotNull()
  }
}
