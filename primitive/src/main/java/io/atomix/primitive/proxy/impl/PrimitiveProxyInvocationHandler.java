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

import com.google.common.collect.Maps;
import io.atomix.primitive.operation.OperationId;
import io.atomix.primitive.proxy.PrimitiveProxyClient;
import io.atomix.primitive.service.PrimitiveService;
import io.atomix.primitive.service.ServiceOperation;
import io.atomix.utils.serializer.Serializer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Primitive proxy invocation handler.
 */
public class PrimitiveProxyInvocationHandler<T extends PrimitiveService> implements InvocationHandler {
  private static final Short DEFAULT_SHORT = (short) 0;
  private static final Integer DEFAULT_INTEGER = 0;
  private static final Long DEFAULT_LONG = 0L;
  private static final Float DEFAULT_FLOAT = 0F;
  private static final Double DEFAULT_DOUBLE = 0D;
  private static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;

  private final PrimitiveProxyClient client;
  private final Serializer serializer;
  private final Map<Method, Function<Object[], CompletableFuture<?>>> processors = Maps.newConcurrentMap();
  private final ThreadLocal<CompletableFuture<?>> localFuture = new ThreadLocal<>();

  public PrimitiveProxyInvocationHandler(PrimitiveProxyClient client, Class<T> serviceClass, Serializer serializer) {
    this.client = client;
    this.serializer = serializer;
    createProcessors(serviceClass);
  }

  private void createProcessors(Class<T> serviceClass) {
    for (Method method : serviceClass.getDeclaredMethods()) {
      Function<Object[], CompletableFuture<?>> processor = createProcessor(method);
      if (processor != null) {
        processors.put(method, processor);
      }
    }
  }

  private Function<Object[], CompletableFuture<?>> createProcessor(Method method) {
    ServiceOperation operation = method.getAnnotation(ServiceOperation.class);
    if (operation == null) {
      return null;
    }

    OperationId operationId = OperationId.from(operation.value(), operation.type());
    if (method.getReturnType().equals(Void.TYPE)) {
      return createVoidProcessor(operationId, method);
    } else {
      return createObjectProcessor(operationId, method);
    }
  }

  private Function<Object[], CompletableFuture<?>> createVoidProcessor(OperationId operationId, Method method) {
    if (method.getParameterTypes().length == 0) {
      return createEmptyVoidProcessor(operationId, method);
    } else if (method.getParameterTypes().length == 1) {
      return createUnaryVoidProcessor(operationId, method);
    } else {
      return createNaryVoidProcessor(operationId, method);
    }
  }

  private Function<Object[], CompletableFuture<?>> createObjectProcessor(OperationId operationId, Method method) {
    if (method.getParameterTypes().length == 0) {
      return createEmptyObjectProcessor(operationId, method);
    } else if (method.getParameterTypes().length == 1) {
      return createUnaryObjectProcessor(operationId, method);
    } else {
      return createNaryObjectProcessor(operationId, method);
    }
  }

  private Function<Object[], CompletableFuture<?>> createEmptyVoidProcessor(OperationId operationId, Method method) {
    return args -> client.invoke(operationId);
  }

  private Function<Object[], CompletableFuture<?>> createUnaryVoidProcessor(OperationId operationId, Method method) {
    return args -> client.invoke(operationId, serializer::encode, args[0]);
  }

  private Function<Object[], CompletableFuture<?>> createNaryVoidProcessor(OperationId operationId, Method method) {
    return args -> client.invoke(operationId, serializer::encode, args);
  }

  private Function<Object[], CompletableFuture<?>> createEmptyObjectProcessor(OperationId operationId, Method method) {
    return args -> client.invoke(operationId, serializer::decode);
  }

  private Function<Object[], CompletableFuture<?>> createUnaryObjectProcessor(OperationId operationId, Method method) {
    return args -> client.invoke(operationId, serializer::encode, args[0], serializer::decode);
  }

  private Function<Object[], CompletableFuture<?>> createNaryObjectProcessor(OperationId operationId, Method method) {
    return args -> client.invoke(operationId, serializer::encode, args, serializer::decode);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().equals("getResponseFuture")) {
      return localFuture.get();
    }

    Function<Object[], CompletableFuture<?>> processor = processors.get(method);
    if (processor == null) {
      throw new AssertionError("Unknown method");
    }

    localFuture.set(processor.apply(args));

    Class<?> returnType = method.getReturnType();
    if (returnType == Short.class || returnType == Short.TYPE) {
      return DEFAULT_SHORT;
    } else if (returnType == Integer.class || returnType == Integer.TYPE) {
      return DEFAULT_INTEGER;
    } else if (returnType == Long.class || returnType == Long.TYPE) {
      return DEFAULT_LONG;
    } else if (returnType == Float.class || returnType == Float.TYPE) {
      return DEFAULT_FLOAT;
    } else if (returnType == Double.class || returnType == Double.TYPE) {
      return DEFAULT_DOUBLE;
    } else if (returnType == Boolean.TYPE) {
      return DEFAULT_BOOLEAN;
    }
    return null;
  }
}
