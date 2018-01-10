/*
 * Copyright 2017-present Open Networking Foundation
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
package io.atomix.primitive.service;

import io.atomix.primitive.PrimitiveId;
import io.atomix.primitive.operation.OperationId;
import io.atomix.primitive.service.impl.DefaultServiceExecutor;
import io.atomix.primitive.session.Session;
import io.atomix.primitive.session.Sessions;
import io.atomix.utils.concurrent.Scheduler;
import io.atomix.utils.logging.ContextualLoggerFactory;
import io.atomix.utils.logging.LoggerContext;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.time.Clock;
import io.atomix.utils.time.LogicalClock;
import io.atomix.utils.time.WallClock;
import io.atomix.utils.time.WallClockTimestamp;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Raft service.
 */
public abstract class AbstractPrimitiveStateMachine implements PrimitiveStateMachine {
  private final Class<? extends PrimitiveService> serviceClass;
  private final Serializer serializer;
  private Logger log;
  private ServiceContext context;
  private ServiceExecutor executor;

  protected AbstractPrimitiveStateMachine() {
    this(null, null);
  }

  protected AbstractPrimitiveStateMachine(Class<? extends PrimitiveService> serviceClass, Serializer serializer) {
    this.serviceClass = serviceClass;
    this.serializer = serializer;
  }

  @Override
  public void init(ServiceContext context) {
    this.context = context;
    this.executor = new DefaultServiceExecutor(context);
    this.log = ContextualLoggerFactory.getLogger(getClass(), LoggerContext.builder(PrimitiveStateMachine.class)
        .addValue(context.serviceId())
        .add("type", context.serviceType())
        .add("name", context.serviceName())
        .build());
    configure(executor);
  }

  @Override
  public void tick(WallClockTimestamp timestamp) {
    executor.tick(timestamp);
  }

  @Override
  public byte[] apply(Commit<byte[]> commit) {
    return executor.apply(commit);
  }

  /**
   * Configures the state machine.
   * <p>
   * By default, this method will configure state machine operations by extracting public methods with
   * a single {@link Commit} parameter via reflection. Override this method to explicitly register
   * state machine operations via the provided {@link ServiceExecutor}.
   *
   * @param executor The state machine executor.
   */
  protected void configure(ServiceExecutor executor) {
    registerOperations(executor);
  }

  /**
   * Registers primitive operations using the given service executor.
   *
   * @param executor the service executor to use to register primitive operations
   */
  private void registerOperations(ServiceExecutor executor) {
    for (Method method : serviceClass.getMethods()) {
      registerOperations(method, executor);
    }
  }

  /**
   * Registers primitive operations for the given method using the given service executor.
   */
  private void registerOperations(Method method, ServiceExecutor executor) {
    ServiceOperation operation = method.getAnnotation(ServiceOperation.class);
    if (operation == null) {
      return;
    }

    OperationId operationId = OperationId.from(operation.value(), operation.type());

    if (method.getReturnType().equals(Void.TYPE)) {
      registerVoidOperation(operationId, method, executor);
    } else {
      registerObjectOperation(operationId, method, executor);
    }
  }

  /**
   * Registers a void operation.
   */
  private void registerVoidOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    if (method.getParameterTypes().length == 0) {
      registerEmptyVoidOperation(operationId, method, executor);
    } else if (method.getParameterTypes().length == 1) {
      registerUnaryVoidOperation(operationId, method, executor);
    } else {
      registerNaryVoidOperation(operationId, method, executor);
    }
  }

  /**
   * Registers an operation with a return value.
   */
  private void registerObjectOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    if (method.getParameterTypes().length == 0) {
      registerEmptyObjectOperation(operationId, method, executor);
    } else if (method.getParameterTypes().length == 1) {
      registerUnaryObjectOperation(operationId, method, executor);
    } else {
      registerNaryObjectOperation(operationId, method, executor);
    }
  }

  /**
   * Registers a no-arg void operation.
   */
  private void registerEmptyVoidOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    executor.register(operationId, () -> {
      try {
        method.invoke(this);
      } catch (InvocationTargetException | IllegalAccessException e) {
        log.error("An error occurred: {}", e);
      }
    });
  }

  /**
   * Registers a single-argument void operation.
   */
  private void registerUnaryVoidOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    executor.register(operationId, serializer::decode, commit -> {
      try {
        method.invoke(this, commit.value());
      } catch (InvocationTargetException | IllegalAccessException e) {
        log.error("An error occurred: {}", e);
      }
    });
  }

  /**
   * Registers a multi-argument void operation.
   */
  private void registerNaryVoidOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    executor.register(operationId, serializer::decode, commit -> {
      try {
        method.invoke(this, (Object[]) commit.value());
      } catch (InvocationTargetException | IllegalAccessException e) {
        log.error("An error occurred: {}", e);
      }
    });
  }

  /**
   * Registers a no-argument operation with a return value.
   */
  private void registerEmptyObjectOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    executor.register(operationId, () -> {
      try {
        return method.invoke(this);
      } catch (InvocationTargetException | IllegalAccessException e) {
        log.error("An error occurred: {}", e);
        throw new RuntimeException(e);
      }
    }, serializer::encode);
  }

  /**
   * Registers a single-argument operation with a return value.
   */
  private void registerUnaryObjectOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    executor.register(operationId, serializer::decode, commit -> {
      try {
        return method.invoke(this, commit.value());
      } catch (InvocationTargetException | IllegalAccessException e) {
        log.error("An error occurred: {}", e);
        throw new RuntimeException(e);
      }
    }, serializer::encode);
  }

  /**
   * Registers a multi-argument operation with a return value.
   */
  private void registerNaryObjectOperation(OperationId operationId, Method method, ServiceExecutor executor) {
    executor.register(operationId, serializer::decode, commit -> {
      try {
        return method.invoke(this, (Object[]) commit.value());
      } catch (InvocationTargetException | IllegalAccessException e) {
        log.error("An error occurred: {}", e);
        throw new RuntimeException(e);
      }
    }, serializer::encode);
  }

  /**
   * Returns the service context.
   *
   * @return the service context
   */
  protected ServiceContext getContext() {
    return context;
  }

  /**
   * Returns the service logger.
   *
   * @return the service logger
   */
  protected Logger getLogger() {
    return log;
  }

  /**
   * Returns the state machine scheduler.
   *
   * @return The state machine scheduler.
   */
  protected Scheduler getScheduler() {
    return executor;
  }

  /**
   * Returns the unique state machine identifier.
   *
   * @return The unique state machine identifier.
   */
  protected PrimitiveId getPrimitiveId() {
    return context.serviceId();
  }

  /**
   * Returns the unique state machine name.
   *
   * @return The unique state machine name.
   */
  protected String getPrimitiveName() {
    return context.serviceName();
  }

  /**
   * Returns the state machine's current index.
   *
   * @return The state machine's current index.
   */
  protected long getCurrentIndex() {
    return context.currentIndex();
  }

  /**
   * Returns the current session.
   *
   * @return the current session
   */
  protected Session getCurrentSession() {
    return context.currentSession();
  }

  /**
   * Returns the state machine's clock.
   *
   * @return The state machine's clock.
   */
  protected Clock getClock() {
    return getWallClock();
  }

  /**
   * Returns the state machine's wall clock.
   *
   * @return The state machine's wall clock.
   */
  protected WallClock getWallClock() {
    return context.wallClock();
  }

  /**
   * Returns the state machine's logical clock.
   *
   * @return The state machine's logical clock.
   */
  protected LogicalClock getLogicalClock() {
    return context.logicalClock();
  }

  /**
   * Returns the sessions registered with the state machines.
   *
   * @return The state machine's sessions.
   */
  protected Sessions getSessions() {
    return context.sessions();
  }

  @Override
  public void onOpen(Session session) {

  }

  @Override
  public void onExpire(Session session) {

  }

  @Override
  public void onClose(Session session) {

  }
}
