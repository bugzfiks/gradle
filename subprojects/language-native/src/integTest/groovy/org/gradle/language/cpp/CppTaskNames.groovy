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

trait CppTaskNames {

    private static final String DEBUG = 'Debug'
    private static final String RELEASE = 'Release'

    String[] compileTasksDebug(String project = '') {
        compileTasks(project, DEBUG)
    }

    String linkTaskDebug(String project = '') {
        linkTask(project, DEBUG)
    }

    String installTaskDebug(String project = '') {
        installTask(project, DEBUG)
    }

    String installTaskDebugStatic(String project = '') {
        installTask(project, DEBUG, 'Static')
    }

    String[] compileTasksRelease(String project = '') {
        compileTasks(project, RELEASE)
    }

    String linkTaskRelease(String project = '') {
        linkTask(project, RELEASE)
    }

    String installTaskRelease(String project = '') {
        installTask(project, RELEASE)
    }

    String[] compileTasks(String project = '', String buildType, String linkage = '') {
        ["${project}:depend${buildType}${linkage}Cpp", compileTask(project, buildType, linkage)] as String[]
    }

    String compileTask(String project = '', String buildType, String linkage = '') {
        "${project}:compile${buildType}${linkage}Cpp"
    }

    String linkTask(String project = '', String buildType, String linkage = '') {
        "${project}:link${buildType}${linkage}"
    }

    String createTask(String project = '', String buildType) {
        "${project}:create${buildType}Static"
    }

    String installTask(String project = '', String buildType, String linkage = '') {
        "${project}:install${buildType}${linkage}"
    }

    String[] compileAndLinkTasks(List<String> projects = [''], String buildType) {
        projects.collect { project ->
            [*compileTasks(project, buildType), linkTask(project, buildType)]
        }.flatten()
    }

    String[] compileAndCreateTasks(List<String> projects = [''], String buildType) {
        projects.collect { project ->
            [*compileTasks(project, buildType, 'Static'), createTask(project, buildType)]
        }.flatten()
    }

    String[] compileAndLinkStaticTasks(List<String> projects = [''], String buildType) {
        projects.collect { project ->
            [*compileTasks(project, buildType, 'Static'), linkTask(project, buildType, 'Static')]
        }.flatten()
    }

    String getDebug() {
        DEBUG
    }

    String getRelease() {
        RELEASE
    }

}
