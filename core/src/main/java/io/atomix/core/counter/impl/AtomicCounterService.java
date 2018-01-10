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
package io.atomix.core.counter.impl;

import io.atomix.primitive.operation.OperationType;
import io.atomix.primitive.service.PrimitiveService;
import io.atomix.primitive.service.ServiceOperation;

/**
 * Atomic counter service.
 */
public interface AtomicCounterService extends PrimitiveService {

  /**
   * Handles a set commit.
   *
   * @param commit the commit to handle
   */
  @ServiceOperation(value = "SET", type = OperationType.COMMAND)
  void set(AtomicCounterOperations.Set commit);

  /**
   * Handles a get commit.
   *
   * @return counter value
   */
  @ServiceOperation(value = "GET", type = OperationType.QUERY)
  Long get();

  /**
   * Handles a compare and set commit.
   *
   * @param commit the commit to handle
   * @return counter value
   */
  @ServiceOperation(value = "COMPARE_AND_SET", type = OperationType.COMMAND)
  boolean compareAndSet(AtomicCounterOperations.CompareAndSet commit);

  /**
   * Handles an increment and get commit.
   *
   * @return counter value
   */
  @ServiceOperation(value = "INCREMENT_AND_GET", type = OperationType.COMMAND)
  long incrementAndGet();

  /**
   * Handles a get and increment commit.
   *
   * @return counter value
   */
  @ServiceOperation(value = "GET_AND_INCREMENT", type = OperationType.COMMAND)
  long getAndIncrement();

  /**
   * Handles a decrement and get commit.
   *
   * @return counter value
   */
  @ServiceOperation(value = "DECREMENT_AND_GET", type = OperationType.COMMAND)
  long decrementAndGet();

  /**
   * Handles a get and decrement commit.
   *
   * @return counter value
   */
  @ServiceOperation(value = "GET_AND_DECREMENT", type = OperationType.COMMAND)
  long getAndDecrement();

  /**
   * Handles an add and get commit.
   *
   * @param commit the commit to handle
   * @return counter value
   */
  @ServiceOperation(value = "ADD_AND_GET", type = OperationType.COMMAND)
  long addAndGet(AtomicCounterOperations.AddAndGet commit);

  /**
   * Handles a get and add commit.
   *
   * @param commit the commit to handle
   * @return counter value
   */
  @ServiceOperation(value = "GET_AND_ADD", type = OperationType.COMMAND)
  long getAndAdd(AtomicCounterOperations.GetAndAdd commit);
}
