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
import io.atomix.primitive.impl.AsyncPrimitiveProxy;
import io.atomix.primitive.proxy.PrimitiveProxyClient;
import io.atomix.utils.serializer.KryoNamespace;
import io.atomix.utils.serializer.KryoNamespaces;
import io.atomix.utils.serializer.Serializer;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Atomix counter implementation.
 */
public class AtomicCounterProxy extends AsyncPrimitiveProxy<AtomicCounterService> implements AsyncAtomicCounter {
  private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.builder()
      .register(KryoNamespaces.BASIC)
      .register(AtomicCounterOperations.NAMESPACE)
      .build());

  public AtomicCounterProxy(PrimitiveProxyClient proxy) {
    super(proxy, AtomicCounterService.class, SERIALIZER);
  }

  private long nullOrZero(Long value) {
    return value != null ? value : 0;
  }

  @Override
  public CompletableFuture<Long> get() {
    return proxy.invoke(service -> service.get()).thenApply(this::nullOrZero);
  }

  @Override
  public CompletableFuture<Void> set(long value) {
    return proxy.call(service -> service.set(new Set(value)));
  }

  @Override
  public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
    return proxy.invoke(service -> service.compareAndSet(new CompareAndSet(expectedValue, updateValue)));
  }

  @Override
  public CompletableFuture<Long> addAndGet(long delta) {
    return proxy.invoke(service -> service.addAndGet(new AddAndGet(delta)));
  }

  @Override
  public CompletableFuture<Long> getAndAdd(long delta) {
    return proxy.invoke(service -> service.getAndAdd(new GetAndAdd(delta)));
  }

  @Override
  public CompletableFuture<Long> incrementAndGet() {
    return proxy.invoke(service -> service.incrementAndGet());
  }

  @Override
  public CompletableFuture<Long> getAndIncrement() {
    return proxy.invoke(service -> service.getAndIncrement());
  }

  @Override
  public CompletableFuture<Long> decrementAndGet() {
    return proxy.invoke(service -> service.decrementAndGet());
  }

  @Override
  public CompletableFuture<Long> getAndDecrement() {
    return proxy.invoke(service -> service.getAndDecrement());
  }

  @Override
  public AtomicCounter sync(Duration operationTimeout) {
    return new BlockingAtomicCounter(this, operationTimeout.toMillis());
  }
}