/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.language.cpp

import org.gradle.nativeplatform.fixtures.app.CppAppWithLibraries
import org.gradle.nativeplatform.fixtures.app.CppAppWithLibrariesWithApiDependencies
import org.gradle.nativeplatform.fixtures.app.CppGreeterWithOptionalFeature
import org.gradle.nativeplatform.fixtures.app.CppLib

import static org.gradle.util.Matchers.containsText

class CppLibraryIntegrationTest extends AbstractCppInstalledToolChainIntegrationTest implements CppTaskNames {

    def "skip compile and link tasks when no source"() {
        given:
        buildFile << """
            apply plugin: 'cpp-library'
        """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(compileAndLinkTasks(debug), ":assemble")
        // TODO - should skip the task as NO-SOURCE
        result.assertTasksSkipped(compileAndLinkTasks(debug), ":assemble")
    }

    def "build fails when compilation fails"() {
        given:
        buildFile << """
            apply plugin: 'cpp-library'
         """

        and:
        file("src/main/cpp/broken.cpp") << """
        #include <iostream>

        'broken
"""

        expect:
        fails "assemble"
        failure.assertHasDescription("Execution failed for task ':compileDebugSharedCpp'.")
        failure.assertHasCause("A build operation failed.")
        failure.assertThatCause(containsText("C++ compiler failed while compiling broken.cpp"))
    }

    def "sources are compiled with C++ compiler"() {
        given:
        settingsFile << "rootProject.name = 'hello'"
        def lib = new CppLib()
        lib.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-library'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(compileAndLinkTasks(debug), ":assemble")
        sharedLibrary("build/lib/main/debug/shared/hello").assertExists()
    }

    def "can build debug and release variants of library"() {
        given:
        settingsFile << "rootProject.name = 'hello'"
        def lib = new CppGreeterWithOptionalFeature()
        lib.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-library'
            compileReleaseSharedCpp.macros(WITH_FEATURE: "true")
         """

        expect:
        executer.withArgument("--info")
        succeeds "linkReleaseShared"

        result.assertTasksExecuted(compileAndLinkTasks(release))
        sharedLibrary("build/lib/main/release/shared/hello").assertExists()
        output.contains('compiling with feature enabled')

        executer.withArgument("--info")
        succeeds "linkDebugShared"

        result.assertTasksExecuted(compileAndLinkTasks(debug))
        sharedLibrary("build/lib/main/debug/shared/hello").assertExists()
        !output.contains('compiling with feature enabled')
    }

    def "can build static and shared variants of library"() {
        given:
        settingsFile << "rootProject.name = 'hello'"
        def lib = new CppGreeterWithOptionalFeature()
        lib.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-library'
         """

        expect:
        succeeds "linkDebugShared"

        result.assertTasksExecuted(compileAndLinkTasks(debug))
        sharedLibrary("build/lib/main/debug/shared/hello").assertExists()
        staticLibrary("build/lib/main/debug/static/hello").assertDoesNotExist()

        succeeds "createReleaseStatic"

        result.assertTasksExecuted(compileAndCreateTasks(release))
        sharedLibrary("build/lib/main/release/shared/hello").assertDoesNotExist()
        staticLibrary("build/lib/main/release/static/hello").assertExists()
    }

    def "build logic can change source layout convention"() {
        def lib = new CppLib()
        settingsFile << "rootProject.name = 'hello'"

        given:
        lib.sources.writeToSourceDir(file("srcs"))
        lib.privateHeaders.writeToSourceDir(file("include"))
        lib.publicHeaders.writeToSourceDir(file("pub"))
        file("src/main/public/${lib.greeter.header.sourceFile.name}") << "ignore me!"
        file("src/main/headers/${lib.greeter.privateHeader.sourceFile.name}") << "ignore me!"
        file("src/main/cpp/broken.cpp") << "ignore me!"

        and:
        buildFile << """
            apply plugin: 'cpp-library'
            library {
                source.from 'srcs'
                publicHeaders.from 'pub'
                privateHeaders.from 'include'
            }
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(compileAndLinkTasks(debug), ":assemble")

        sharedLibrary("build/lib/main/debug/shared/hello").assertExists()
    }

    def "build logic can add individual source files"() {
        def lib = new CppLib()
        settingsFile << "rootProject.name = 'hello'"

        given:
        lib.headers.writeToProject(testDirectory)
        lib.greeter.source.writeToSourceDir(file("src/one.cpp"))
        lib.sum.source.writeToSourceDir(file("src/two.cpp"))
        file("src/main/cpp/broken.cpp") << "ignore me!"

        and:
        buildFile << """
            apply plugin: 'cpp-library'
            library {
                source {
                    from('src/one.cpp')
                    from('src/two.cpp')
                }
            }
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(compileAndLinkTasks(debug), ":assemble")

        sharedLibrary("build/lib/main/debug/shared/hello").assertExists()
    }

    def "honors changes to buildDir"() {
        given:
        settingsFile << "rootProject.name = 'hello'"
        def lib = new CppLib()
        lib.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-library'
            buildDir = 'output'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(compileAndLinkTasks(debug), ":assemble")

        !file("build").exists()
        file("output/obj/main/debug/shared").assertIsDir()
        sharedLibrary("output/lib/main/debug/shared/hello").assertExists()
    }

    def "honors changes to task output locations"() {
        given:
        settingsFile << "rootProject.name = 'hello'"
        def lib = new CppLib()
        lib.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-library'
            compileDebugSharedCpp.objectFileDir = layout.buildDirectory.dir("object-files")
            linkDebugShared.binaryFile = layout.buildDirectory.file("some-lib/main.bin")
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(compileAndLinkTasks(debug), ":assemble")

        file("build/object-files").assertIsDir()
        file("build/some-lib/main.bin").assertIsFile()
    }

    def "library can define public and implementation headers"() {
        given:
        settingsFile << "rootProject.name = 'hello'"
        def lib = new CppLib()
        lib.greeter.header.writeToSourceDir(file("src/main/public"))
        lib.greeter.privateHeader.writeToSourceDir(file("src/main/headers"))
        lib.sum.header.writeToSourceDir(file("src/main/public"))
        lib.sources.writeToProject(testDirectory)

        and:
        buildFile << """
            apply plugin: 'cpp-library'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(compileAndLinkTasks(debug), ":assemble")
        sharedLibrary("build/lib/main/debug/shared/hello").assertExists()
    }

    def "can compile and link against implementation and api libraries"() {
        settingsFile << "include 'lib1', 'lib2', 'lib3'"
        def app = new CppAppWithLibrariesWithApiDependencies()

        given:
        buildFile << """
            project(':lib1') {
                apply plugin: 'cpp-library'
                dependencies {
                    api project(':lib2')
                    implementation project(':lib3')
                }
            }
            project(':lib2') {
                apply plugin: 'cpp-library'
            }
            project(':lib3') {
                apply plugin: 'cpp-library'
            }
"""
        app.deck.writeToProject(file("lib1"))
        app.card.writeToProject(file("lib2"))
        app.shuffle.writeToProject(file("lib3"))

        expect:
        succeeds ":lib1:assemble"

        result.assertTasksExecuted(compileAndLinkTasks([':lib3', ':lib2', ':lib1'], debug), ":lib1:assemble")
        sharedLibrary("lib1/build/lib/main/debug/shared/lib1").assertExists()
        sharedLibrary("lib2/build/lib/main/debug/shared/lib2").assertExists()
        sharedLibrary("lib3/build/lib/main/debug/shared/lib3").assertExists()

        succeeds ":lib1:linkReleaseShared"

        result.assertTasksExecuted compileAndLinkTasks([':lib3', ':lib2', ':lib1'], release)
        sharedLibrary("lib1/build/lib/main/release/shared/lib1").assertExists()
        sharedLibrary("lib2/build/lib/main/release/shared/lib2").assertExists()
        sharedLibrary("lib3/build/lib/main/release/shared/lib3").assertExists()
    }

    def "can change default base name and successfully link against library"() {
        settingsFile << "include 'lib1', 'lib2'"
        def app = new CppAppWithLibraries()

        given:
        buildFile << """
            project(':lib1') {
                apply plugin: 'cpp-library'
                library {
                    baseName = 'hello'
                }
                dependencies {
                    implementation project(':lib2')
                }
            }
            project(':lib2') {
                apply plugin: 'cpp-library'
                library {
                    baseName = 'log'
                }
            }
"""
        app.greeterLib.writeToProject(file("lib1"))
        app.loggerLib.writeToProject(file("lib2"))

        expect:
        succeeds ":lib1:assemble"
        result.assertTasksExecuted(compileAndLinkTasks([':lib2', ':lib1'], debug), ":lib1:assemble")
        sharedLibrary("lib1/build/lib/main/debug/shared/hello").assertExists()
        sharedLibrary("lib2/build/lib/main/debug/shared/log").assertExists()
    }

}
