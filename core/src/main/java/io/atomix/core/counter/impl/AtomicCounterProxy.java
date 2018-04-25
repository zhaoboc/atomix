/*
 * Copyright 2016-present Open Networking Foundation
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

import io.atomix.core.counter.AsyncAtomicCounter;
import io.atomix.core.counter.AtomicCounter;
import io.atomix.core.counter.impl.AtomicCounterOperations.AddAndGet;
import io.atomix.core.counter.impl.AtomicCounterOperations.CompareAndSet;
import io.atomix.core.counter.impl.AtomicCounterOperations.GetAndAdd;
import io.atomix.core.counter.impl.AtomicCounterOperations.Set;
import io.atomix.primitive.PrimitiveRegistry;
import io.atomix.primitive.impl.AbstractAsyncPrimitive;
import io.atomix.primitive.proxy.PrimitiveProxy;
import io.atomix.utils.serializer.KryoNamespace;
import io.atomix.utils.serializer.KryoNamespaces;
import io.atomix.utils.serializer.Serializer;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static io.atomix.core.counter.impl.AtomicCounterOperations.ADD_AND_GET;
import static io.atomix.core.counter.impl.AtomicCounterOperations.COMPARE_AND_SET;
import static io.atomix.core.counter.impl.AtomicCounterOperations.DECREMENT_AND_GET;
import static io.atomix.core.counter.impl.AtomicCounterOperations.GET;
import static io.atomix.core.counter.impl.AtomicCounterOperations.GET_AND_ADD;
import static io.atomix.core.counter.impl.AtomicCounterOperations.GET_AND_DECREMENT;
import static io.atomix.core.counter.impl.AtomicCounterOperations.GET_AND_INCREMENT;
import static io.atomix.core.counter.impl.AtomicCounterOperations.INCREMENT_AND_GET;
import static io.atomix.core.counter.impl.AtomicCounterOperations.SET;

/**
 * Atomix counter implementation.
 */
public class AtomicCounterProxy extends AbstractAsyncPrimitive<AsyncAtomicCounter> implements AsyncAtomicCounter {
  private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.builder()
      .register(KryoNamespaces.BASIC)
      .register(AtomicCounterOperations.NAMESPACE)
      .build());

  public AtomicCounterProxy(PrimitiveProxy proxy, PrimitiveRegistry registry) {
    super(proxy, registry);
  }

  private long nullOrZero(Long value) {
    return value != null ? value : 0;
  }

  @Override
  public CompletableFuture<Long> get() {
    return this.<Long>invoke(getPartitionKey(), GET, SERIALIZER::decode).thenApply(this::nullOrZero);
  }

  @Override
  public CompletableFuture<Void> set(long value) {
    return this.invoke(getPartitionKey(), SET, SERIALIZER::encode, new Set(value));
  }

  @Override
  public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
    return this.invoke(getPartitionKey(), COMPARE_AND_SET, SERIALIZER::encode,
        new CompareAndSet(expectedValue, updateValue), SERIALIZER::decode);
  }

  @Override
  public CompletableFuture<Long> addAndGet(long delta) {
    return this.invoke(getPartitionKey(), ADD_AND_GET, SERIALIZER::encode, new AddAndGet(delta), SERIALIZER::decode);
  }

  @Override
  public CompletableFuture<Long> getAndAdd(long delta) {
    return this.invoke(getPartitionKey(), GET_AND_ADD, SERIALIZER::encode, new GetAndAdd(delta), SERIALIZER::decode);
  }

  @Override
  public CompletableFuture<Long> incrementAndGet() {
    return this.invoke(getPartitionKey(), INCREMENT_AND_GET, SERIALIZER::decode);
  }

  @Override
  public CompletableFuture<Long> getAndIncrement() {
    return this.invoke(getPartitionKey(), GET_AND_INCREMENT, SERIALIZER::decode);
  }

  @Override
  public CompletableFuture<Long> decrementAndGet() {
    return this.invoke(getPartitionKey(), DECREMENT_AND_GET, SERIALIZER::decode);
  }

  @Override
  public CompletableFuture<Long> getAndDecrement() {
    return this.invoke(getPartitionKey(), GET_AND_DECREMENT, SERIALIZER::decode);
  }

  @Override
  public AtomicCounter sync(Duration operationTimeout) {
    return new BlockingAtomicCounter(this, operationTimeout.toMillis());
  }
}