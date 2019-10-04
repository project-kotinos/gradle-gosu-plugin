/*
 * Copyright 2018 the original author or authors.
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

package org.gosulang.gradle.tasks.compile.incremental.recomp;

import org.gosulang.gradle.tasks.compile.incremental.classpath.ClasspathEntrySnapshot;
import org.gosulang.gradle.tasks.compile.incremental.classpath.ClasspathEntrySnapshotCache;
import org.gosulang.gradle.tasks.compile.incremental.deps.ClassSetAnalysis;
import org.gosulang.gradle.tasks.compile.incremental.deps.DependentsSet;


import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class PreviousCompilation {
  private final PreviousCompilationData data;
  private final ClasspathEntrySnapshotCache classpathEntrySnapshotCache;
  private final PreviousCompilationOutputAnalyzer previousCompilationOutputAnalyzer;
  private ClassSetAnalysis classAnalysis;

  public PreviousCompilation(PreviousCompilationData data, ClasspathEntrySnapshotCache classpathEntrySnapshotCache, PreviousCompilationOutputAnalyzer previousCompilationOutputAnalyzer) {
    this.data = data;
    this.classpathEntrySnapshotCache = classpathEntrySnapshotCache;
    this.previousCompilationOutputAnalyzer = previousCompilationOutputAnalyzer;
  }

  public DependentsSet getDependents(Set<String> allClasses, Set<Integer> constants) {
    return getClassAnalysis().getRelevantDependents(allClasses, constants);
  }

  private ClassSetAnalysis getClassAnalysis() {
    if (classAnalysis == null) {
      classAnalysis = previousCompilationOutputAnalyzer.getAnalysis(data.getDestinationDir());
    }

    return classAnalysis;
  }

  public ClasspathEntrySnapshot getClasspathEntrySnapshot(File file) {
    return classpathEntrySnapshotCache.get(file, data.getClasspathSnapshot().getFileHashes().get(file));
  }

  public Set<File> getClasspath() {
    return data.getClasspathSnapshot().getFileHashes().keySet();
  }

  public DependentsSet getDependents(String className, Set<Integer> newConstants) {
    Set<Integer> constants = new HashSet<>(getClassAnalysis().getConstants(className));
    constants.removeAll(newConstants);
    return getClassAnalysis().getRelevantDependents(className, constants);
  }

  public Set<String> getTypesToReprocess() {
    return getClassAnalysis().getTypesToReprocess();
  }
}
