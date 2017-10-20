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

package org.gradle.nativeplatform.test.cpp.plugins;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.plugins.CppBasePlugin;
import org.gradle.language.cpp.plugins.CppExecutablePlugin;
import org.gradle.language.cpp.plugins.CppLibraryPlugin;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.tasks.AbstractLinkTask;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;
import org.gradle.nativeplatform.test.cpp.internal.DefaultCppTestSuite;

import javax.inject.Inject;


/**
 * A plugin that sets up the infrastructure for testing C++ binaries.
 *
 * It also adds conventions on top of it.
 *
 * @since 4.4
 */
@Incubating
public class CppUnitTestBasePlugin implements Plugin<ProjectInternal> {
    private final ObjectFactory objectFactory;
    private final FileOperations fileOperations;

    @Inject
    public CppUnitTestBasePlugin(FileOperations fileOperations, ObjectFactory objectFactory) {
        this.fileOperations = fileOperations;
        this.objectFactory = objectFactory;
    }

    @Override
    public void apply(final ProjectInternal project) {
        project.getPluginManager().apply(CppBasePlugin.class);

        final ConfigurationContainer configurations = project.getConfigurations();

        final CppTestSuite testComponent = objectFactory.newInstance(DefaultCppTestSuite.class, "unitTest", objectFactory, fileOperations, configurations);
        // Register components created for the test Component and test binaries
        project.getComponents().add(testComponent);
        project.getComponents().add(testComponent.getDevelopmentBinary());
        project.getExtensions().add(CppTestSuite.class, "unitTest", testComponent);

        Action<Plugin<ProjectInternal>> projectConfiguration = new Action<Plugin<ProjectInternal>>() {
            @Override
            public void execute(Plugin<ProjectInternal> plugin) {
                final TaskContainer tasks = project.getTasks();
                CppComponent mainComponent = project.getComponents().withType(CppComponent.class).findByName("main");
                testComponent.getTestedComponent().set(mainComponent);

                AbstractLinkTask linkTest = tasks.withType(AbstractLinkTask.class).getByName("linkUnitTestExecutable");
                //linkTest.source(mainComponent.getDevelopmentBinary());

                CppCompile compileMain = tasks.withType(CppCompile.class).getByName("compileDebugCpp");
                linkTest.source(compileMain.getObjectFileDir().getAsFileTree().matching(new PatternSet().include("**/*.obj", "**/*.o")));
            }
        };

        project.getPlugins().withType(CppLibraryPlugin.class, projectConfiguration);
        // TODO: We will get symbol conflicts with executables since they already have a main()
        project.getPlugins().withType(CppExecutablePlugin.class, projectConfiguration);

    }
}
