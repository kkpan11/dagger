# Copyright (C) 2021 The Dagger Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("//:build_defs.bzl", "DOCLINT_HTML_AND_SYNTAX")
load("//:test_defs.bzl", "GenJavaTests")

package(default_visibility = ["//:src"])

# TODO(b/203233586): Replace with GenJavaTest
GenJavaTests(
    name = "OptionalBindingComponentsEmptyTest",
    srcs = [
        "OptionalBindingComponentsWithInaccessibleTypes.java",
        "OptionalBindingComponentsWithInaccessibleTypesTest.java",
    ],
    javacopts = DOCLINT_HTML_AND_SYNTAX,
    test_only_deps = [
        
        "//java/dagger/internal/guava:base",
        "//java/dagger/internal/guava:collect",
        "@google_bazel_common//third_party/java/junit",
        "@google_bazel_common//third_party/java/truth",
    ],
    deps = [
        "@google_bazel_common//third_party/java/auto:value",
        "//:dagger_with_compiler",
        "@google_bazel_common//third_party/java/jsr305_annotations",
        "//javatests/dagger/functional/jdk8:optional_binding_components",
    ],
)