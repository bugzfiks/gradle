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

import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.internal.DefaultCppApplication;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;

import javax.inject.Inject;

public class DefaultCppTestSuite extends DefaultCppApplication implements CppTestSuite {
    private final Property<CppComponent> testedComponent;
    private final CppTestExecutable testBinary;

    @Inject
    public DefaultCppTestSuite(String name, ObjectFactory objectFactory, final FileOperations fileOperations, ConfigurationContainer configurations) {
        super(name, objectFactory, fileOperations, configurations);
        this.testedComponent = objectFactory.property(CppComponent.class);
        this.testBinary = objectFactory.newInstance(DefaultCppTestExecutable.class, name + "Executable", objectFactory, getBaseName(), true, getCppSource(), getPrivateHeaderDirs(), configurations, getImplementationDependencies(), getTestedComponent());
        getBaseName().set(name);
    }

    @Override
    public Property<CppComponent> getTestedComponent() {
        return testedComponent;
    }

    @Override
    public CppTestExecutable getDevelopmentBinary() {
        return testBinary;
    }
}
