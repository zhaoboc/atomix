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
package io.atomix.primitive.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.atomix.primitive.AsyncPrimitive;
import io.atomix.primitive.proxy.PrimitiveProxyClient;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract base class for primitives that interact with Raft replicated state machines via proxy.
 */
public abstract class AsyncPrimitiveClient implements AsyncPrimitive {
  private final Function<PrimitiveProxyClient.State, Status> mapper = state -> {
    switch (state) {
      case CONNECTED:
        return Status.ACTIVE;
      case SUSPENDED:
        return Status.SUSPENDED;
      case CLOSED:
        return Status.INACTIVE;
      default:
        throw new IllegalStateException("Unknown state " + state);
    }
  };

  protected final PrimitiveProxyClient client;
  private final Set<Consumer<Status>> statusChangeListeners = Sets.newCopyOnWriteArraySet();

  public AsyncPrimitiveClient(PrimitiveProxyClient client) {
    this.client = checkNotNull(client, "proxy cannot be null");
    client.addStateChangeListener(this::onStateChange);
  }

  @Override
  public String name() {
    return client.name();
  }

  /**
   * Handles a Raft session state change.
   *
   * @param state the updated Raft session state
   */
  private void onStateChange(PrimitiveProxyClient.State state) {
    statusChangeListeners.forEach(listener -> listener.accept(mapper.apply(state)));
  }

  @Override
  public void addStatusChangeListener(Consumer<Status> listener) {
    statusChangeListeners.add(listener);
  }

  @Override
  public void removeStatusChangeListener(Consumer<Status> listener) {
    statusChangeListeners.remove(listener);
  }

  @Override
  public Collection<Consumer<Status>> statusChangeListeners() {
    return ImmutableSet.copyOf(statusChangeListeners);
  }

  @Override
  public CompletableFuture<Void> close() {
    return client.close();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("proxy", client)
        .toString();
  }
}