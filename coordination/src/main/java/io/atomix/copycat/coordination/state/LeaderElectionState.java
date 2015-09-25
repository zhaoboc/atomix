/*
 * Copyright 2015 the original author or authors.
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
package io.atomix.copycat.coordination.state;

import io.atomix.catalogue.client.session.Session;
import io.atomix.catalogue.server.Commit;
import io.atomix.catalogue.server.StateMachine;
import io.atomix.catalogue.server.StateMachineExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Leader election state machine.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class LeaderElectionState extends StateMachine {
  private Session leader;
  private long epoch;
  private final List<Commit<LeaderElectionCommands.Listen>> listeners = new ArrayList<>();

  @Override
  public void configure(StateMachineExecutor executor) {
    executor.register(LeaderElectionCommands.Listen.class, this::listen);
    executor.register(LeaderElectionCommands.Unlisten.class, this::unlisten);
    executor.register(LeaderElectionCommands.IsLeader.class, this::isLeader);
  }

  @Override
  public void close(Session session) {
    if (leader != null && leader.equals(session)) {
      leader = null;
      if (!listeners.isEmpty()) {
        Commit<LeaderElectionCommands.Listen> leader = listeners.remove(0);
        this.leader = leader.session();
        this.epoch = leader.index();
        this.leader.publish("elect", true);
      }
    }
  }

  /**
   * Applies listen commits.
   */
  protected void listen(Commit<LeaderElectionCommands.Listen> commit) {
    if (leader == null) {
      leader = commit.session();
      epoch = commit.index();
      leader.publish("elect", epoch);
      commit.clean();
    } else {
      listeners.add(commit);
    }
  }

  /**
   * Applies listen commits.
   */
  protected void unlisten(Commit<LeaderElectionCommands.Unlisten> commit) {
    if (leader != null && leader.equals(commit.session())) {
      leader = null;
      if (!listeners.isEmpty()) {
        Commit<LeaderElectionCommands.Listen> leader = listeners.remove(0);
        this.leader = leader.session();
        this.epoch = commit.index();
        this.leader.publish("elect", epoch);
        leader.clean();
      }
    } else {
      commit.clean();
    }
  }

  /**
   * Applies an isLeader query.
   */
  protected boolean isLeader(Commit<LeaderElectionCommands.IsLeader> commit) {
    return leader != null && leader.equals(commit.session()) && epoch == commit.operation().epoch();
  }

}
