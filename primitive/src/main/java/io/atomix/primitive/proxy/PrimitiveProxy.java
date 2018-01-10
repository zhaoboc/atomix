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
package io.atomix.primitive.proxy;

import io.atomix.primitive.service.PrimitiveService;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Primitive proxy.
 */
public interface PrimitiveProxy<T extends PrimitiveService> {

  /**
   * Invokes a function on the underlying primitive.
   *
   * @param consumer the consumer to apply
   * @return a future to be completed with the function result
   */
  CompletableFuture<Void> call(Consumer<T> consumer);

  /**
   * Invokes a function on the underlying primitive.
   *
   * @param function the primitive service function to invoke
   * @param <U> the function return type
   * @return a future to be completed with the function result
   */
  <U> CompletableFuture<U> invoke(Function<T, U> function);

}
