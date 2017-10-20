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

package org.gradle.nativeplatform.test.cpp.internal;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.internal.DefaultCppExecutable;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class DefaultCppTestExecutable extends DefaultCppExecutable implements CppTestExecutable {
    private final Provider<CppComponent> testedComponent;

    @Inject
    public DefaultCppTestExecutable(String name, ObjectFactory objects, Provider<String> baseName, boolean debuggable, FileCollection sourceFiles, FileCollection componentHeaderDirs, ConfigurationContainer configurations, Configuration implementation, Provider<CppComponent> testedComponent) {
        super(name, objects, baseName, debuggable, sourceFiles, componentHeaderDirs, configurations, implementation);
        this.testedComponent = testedComponent;
    }

    @Override
    public FileCollection getCompileIncludePath() {
        // TODO: This should be modelled different, perhaps as a dependency on the implementation configuration
        return super.getCompileIncludePath().plus(getFileOperations().files(new Callable<FileCollection>() {
            @Override
            public FileCollection call() throws Exception {
                CppComponent tested = testedComponent.getOrNull();
                if (tested == null) {
                    return getFileOperations().files();
                }
                return tested.getAllHeaderDirs();
            }
        }));
    }
}
