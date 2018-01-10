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
package io.atomix.primitive.impl;

import io.atomix.primitive.proxy.PrimitiveProxy;
import io.atomix.primitive.proxy.PrimitiveProxyClient;
import io.atomix.primitive.proxy.impl.DefaultPrimitiveProxy;
import io.atomix.primitive.service.PrimitiveService;
import io.atomix.utils.serializer.Serializer;

/**
 * Base class for async primitive proxies.
 */
public abstract class AsyncPrimitiveProxy<T extends PrimitiveService> extends AsyncPrimitiveClient {
  protected final PrimitiveProxy<T> proxy;

  public AsyncPrimitiveProxy(PrimitiveProxyClient client, Class<T> serviceType, Serializer serializer) {
    super(client);
    this.proxy = new DefaultPrimitiveProxy<T>(client, serviceType, serializer);
  }
}
