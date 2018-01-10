/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.primitive.proxy.impl;

import io.atomix.primitive.proxy.PrimitiveProxy;
import io.atomix.primitive.proxy.PrimitiveProxyClient;
import io.atomix.primitive.service.PrimitiveService;
import io.atomix.utils.serializer.Serializer;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Default primitive proxy.
 */
public class DefaultPrimitiveProxy<T extends PrimitiveService> implements PrimitiveProxy<T> {
  private final T serviceProxy;
  private final PrimitiveServiceResponder serviceResponder;

  @SuppressWarnings("unchecked")
  public DefaultPrimitiveProxy(PrimitiveProxyClient client, Class<T> serviceClass, Serializer serializer) {
    this.serviceProxy = (T) Proxy.newProxyInstance(
        serviceClass.getClassLoader(),
        new Class[]{serviceClass, PrimitiveServiceResponder.class},
        new PrimitiveProxyInvocationHandler<>(client, serviceClass, serializer));
    this.serviceResponder = (PrimitiveServiceResponder) serviceProxy;
  }

  @Override
  public CompletableFuture<Void> call(Consumer<T> consumer) {
    consumer.accept(serviceProxy);
    return serviceResponder.getResponseFuture();
  }

  @Override
  public <U> CompletableFuture<U> invoke(Function<T, U> function) {
    function.apply(serviceProxy);
    return serviceResponder.getResponseFuture();
  }
}
