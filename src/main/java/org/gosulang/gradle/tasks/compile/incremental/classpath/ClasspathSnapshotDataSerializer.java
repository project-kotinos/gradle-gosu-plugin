/*
 * Copyright 2014 the original author or authors.
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

package org.gosulang.gradle.tasks.compile.incremental.classpath;

import org.gradle.internal.hash.HashCode;
import org.gradle.internal.serialize.AbstractSerializer;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.HashCodeSerializer;
import org.gradle.internal.serialize.MapSerializer;
import org.gradle.internal.serialize.SetSerializer;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.gradle.internal.serialize.BaseSerializerFactory.FILE_SERIALIZER;
import static org.gradle.internal.serialize.BaseSerializerFactory.STRING_SERIALIZER;

public class ClasspathSnapshotDataSerializer extends AbstractSerializer<ClasspathSnapshotData> {
  private final MapSerializer<File, HashCode> mapSerializer = new MapSerializer<>(FILE_SERIALIZER, new HashCodeSerializer());
  private final SetSerializer<String> setSerializer = new SetSerializer<>(STRING_SERIALIZER, false);

  @Override
  public ClasspathSnapshotData read(Decoder decoder) throws Exception {
    Set<String> duplicates = setSerializer.read(decoder);
    Map<File, HashCode> hashes = mapSerializer.read(decoder);
    return new ClasspathSnapshotData(hashes, duplicates);
  }

  @Override
  public void write(Encoder encoder, ClasspathSnapshotData value) throws Exception {
    setSerializer.write(encoder, value.getDuplicateClasses());
    mapSerializer.write(encoder, value.getFileHashes());
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }

    ClasspathSnapshotDataSerializer rhs = (ClasspathSnapshotDataSerializer) obj;
    return equal(mapSerializer, rhs.mapSerializer) && equal(setSerializer, rhs.setSerializer);
  }

  @Override
  public int hashCode() {
    return hashCode(super.hashCode(), mapSerializer, setSerializer);
  }

  private static boolean equal(@Nullable Object a, @Nullable Object b) {
    return a == b || a != null && a.equals(b);
  }

  private static int hashCode(@Nullable Object... objects) {
    return Arrays.hashCode(objects);
  }

}
