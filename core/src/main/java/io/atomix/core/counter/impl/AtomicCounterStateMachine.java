/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.core.counter.impl;

import io.atomix.core.counter.impl.AtomicCounterOperations.AddAndGet;
import io.atomix.core.counter.impl.AtomicCounterOperations.CompareAndSet;
import io.atomix.core.counter.impl.AtomicCounterOperations.GetAndAdd;
import io.atomix.core.counter.impl.AtomicCounterOperations.Set;
import io.atomix.primitive.service.AbstractPrimitiveStateMachine;
import io.atomix.storage.buffer.BufferInput;
import io.atomix.storage.buffer.BufferOutput;
import io.atomix.utils.serializer.KryoNamespace;
import io.atomix.utils.serializer.KryoNamespaces;
import io.atomix.utils.serializer.Serializer;

import java.util.Objects;

/**
 * Atomix long state.
 */
public class AtomicCounterStateMachine extends AbstractPrimitiveStateMachine implements AtomicCounterService {
  private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.builder()
      .register(KryoNamespaces.BASIC)
      .register(AtomicCounterOperations.NAMESPACE)
      .build());

  private Long value = 0L;

  public AtomicCounterStateMachine() {
    super(AtomicCounterService.class, SERIALIZER);
  }

  @Override
  public void backup(BufferOutput writer) {
    writer.writeLong(value);
  }

  @Override
  public void restore(BufferInput reader) {
    value = reader.readLong();
  }

  @Override
  public void set(Set commit) {
    value = commit.value();
  }

  @Override
  public Long get() {
    return value;
  }

  @Override
  public boolean compareAndSet(CompareAndSet commit) {
    if (Objects.equals(value, commit.expect())) {
      value = commit.update();
      return true;
    }
    return false;
  }

  @Override
  public long incrementAndGet() {
    Long oldValue = value;
    value = oldValue + 1;
    return value;
  }

  @Override
  public long getAndIncrement() {
    Long oldValue = value;
    value = oldValue + 1;
    return oldValue;
  }

  @Override
  public long decrementAndGet() {
    Long oldValue = value;
    value = oldValue - 1;
    return value;
  }

  @Override
  public long getAndDecrement() {
    Long oldValue = value;
    value = oldValue - 1;
    return oldValue;
  }

  @Override
  public long addAndGet(AddAndGet commit) {
    Long oldValue = value;
    value = oldValue + commit.delta();
    return value;
  }

  @Override
  public long getAndAdd(GetAndAdd commit) {
    Long oldValue = value;
    value = oldValue + commit.delta();
    return oldValue;
  }
}