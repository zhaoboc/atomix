/*
 * Copyright 2014 the original author or authors.
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
package net.kuujo.copycat.test.integration;

import net.kuujo.copycat.Command;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.impl.DefaultCommand;
import net.kuujo.copycat.log.CommandEntry;
import net.kuujo.copycat.log.ConfigurationEntry;
import net.kuujo.copycat.log.Entry;
import net.kuujo.copycat.replication.StateMachine;
import net.kuujo.copycat.replication.node.RaftNode;
import net.kuujo.copycat.replication.node.impl.DefaultRaftNode;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.assertFalse;
import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.vertx.testtools.TestVerticle;

/**
 * A replication test.
 *
 * @author Jordan Halterman
 */
public class ReplicationTest extends TestVerticle {

  private final StateMachine stateMachine = new StateMachine() {
    @Override
    public Object applyCommand(Command command) {
      return null;
    }
  };

  @Test
  public void testNoOpReplication() {
    final ClusterConfig config = new ClusterConfig("test.1", "test.2", "test.3");

    final RaftNode test1 = new DefaultRaftNode("test.1", vertx, stateMachine).setClusterConfig(config);
    test1.start(new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> result) {
        assertTrue(result.succeeded());

        final RaftNode test2 = new DefaultRaftNode("test.2", vertx, stateMachine).setClusterConfig(config);
        test2.start(new Handler<AsyncResult<Void>>() {
          @Override
          public void handle(AsyncResult<Void> result) {
            assertTrue(result.succeeded());

            final RaftNode test3 = new DefaultRaftNode("test.3", vertx, stateMachine).setClusterConfig(config);
            test3.start(new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(AsyncResult<Void> result) {
                assertTrue(result.succeeded());
                test3.submitCommand(new DefaultCommand("test", new JsonObject().putString("data", "Hello world!")), new Handler<AsyncResult<Void>>() {
                  @Override
                  public void handle(AsyncResult<Void> result) {
                    assertTrue(result.succeeded());
                    test1.getLog().entry(0, new Handler<AsyncResult<Entry>>() {
                      @Override
                      public void handle(AsyncResult<Entry> result) {
                        assertTrue(result.succeeded());
                        assertEquals(Entry.Type.NOOP, result.result().type());
                      }
                    });
                    test2.getLog().entry(0, new Handler<AsyncResult<Entry>>() {
                      @Override
                      public void handle(AsyncResult<Entry> result) {
                        assertTrue(result.succeeded());
                        assertEquals(Entry.Type.NOOP, result.result().type());
                      }
                    });
                    test3.getLog().entry(0, new Handler<AsyncResult<Entry>>() {
                      @Override
                      public void handle(AsyncResult<Entry> result) {
                        assertTrue(result.succeeded());
                        assertEquals(Entry.Type.NOOP, result.result().type());
                        testComplete();
                      }
                    });
                  }
                });
              }
            });
          }
        });
      }
    });
  }

  @Test
  public void testCommandReplication() {
    final ClusterConfig config = new ClusterConfig("test.1", "test.2", "test.3");

    final RaftNode test1 = new DefaultRaftNode("test.1", vertx, stateMachine).setClusterConfig(config);
    test1.start(new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> result) {
        assertTrue(result.succeeded());

        final RaftNode test2 = new DefaultRaftNode("test.2", vertx, stateMachine).setClusterConfig(config);
        test2.start(new Handler<AsyncResult<Void>>() {
          @Override
          public void handle(AsyncResult<Void> result) {
            assertTrue(result.succeeded());

            final RaftNode test3 = new DefaultRaftNode("test.3", vertx, stateMachine).setClusterConfig(config);
            test3.start(new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(AsyncResult<Void> result) {
                assertTrue(result.succeeded());
                test3.submitCommand(new DefaultCommand("test", new JsonObject().putString("data", "Hello world!")), new Handler<AsyncResult<Void>>() {
                  @Override
                  public void handle(AsyncResult<Void> result) {
                    assertTrue(result.succeeded());
                    test1.getLog().entry(1, new Handler<AsyncResult<Entry>>() {
                      @Override
                      public void handle(AsyncResult<Entry> result) {
                        assertTrue(result.succeeded());
                        assertEquals(Entry.Type.COMMAND, result.result().type());
                        assertEquals("test", ((CommandEntry) result.result()).command().command());
                        assertEquals("Hello world!", ((CommandEntry) result.result()).command().args().getString("data"));
                      }
                    });
                    test2.getLog().entry(1, new Handler<AsyncResult<Entry>>() {
                      @Override
                      public void handle(AsyncResult<Entry> result) {
                        assertTrue(result.succeeded());
                        assertEquals(Entry.Type.COMMAND, result.result().type());
                        assertEquals("test", ((CommandEntry) result.result()).command().command());
                        assertEquals("Hello world!", ((CommandEntry) result.result()).command().args().getString("data"));
                      }
                    });
                    test3.getLog().entry(1, new Handler<AsyncResult<Entry>>() {
                      @Override
                      public void handle(AsyncResult<Entry> result) {
                        assertTrue(result.succeeded());
                        assertEquals(Entry.Type.COMMAND, result.result().type());
                        assertEquals("test", ((CommandEntry) result.result()).command().command());
                        assertEquals("Hello world!", ((CommandEntry) result.result()).command().args().getString("data"));
                        testComplete();
                      }
                    });
                  }
                });
              }
            });
          }
        });
      }
    });
  }

  @Test
  public void testConfigReplication() {
    final ClusterConfig config = new ClusterConfig("test.1", "test.2", "test.3");

    final RaftNode test1 = new DefaultRaftNode("test.1", vertx, stateMachine)
      .setClusterConfig(config).setHeartbeatInterval(100);
    test1.start(new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> result) {
        assertTrue(result.succeeded());

        final RaftNode test2 = new DefaultRaftNode("test.2", vertx, stateMachine)
          .setClusterConfig(config).setHeartbeatInterval(100);
        test2.start(new Handler<AsyncResult<Void>>() {
          @Override
          public void handle(AsyncResult<Void> result) {
            assertTrue(result.succeeded());

            final RaftNode test3 = new DefaultRaftNode("test.3", vertx, stateMachine)
              .setClusterConfig(config).setHeartbeatInterval(100);
            test3.start(new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(AsyncResult<Void> result) {
                assertTrue(result.succeeded());
                config.addMember("test.4");
                test3.submitCommand(new DefaultCommand("test", new JsonObject().putString("data", "Hello world!")), new Handler<AsyncResult<Void>>() {
                  @Override
                  public void handle(AsyncResult<Void> result) {
                    vertx.setTimer(500, new Handler<Long>() {
                      @Override
                      public void handle(Long event) {
                        test1.getLog().entry(2, new Handler<AsyncResult<Entry>>() {
                          @Override
                          public void handle(AsyncResult<Entry> result) {
                            assertTrue(result.succeeded());
                            assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                          }
                        });
                        test1.getLog().entry(4, new Handler<AsyncResult<Entry>>() {
                          @Override
                          public void handle(AsyncResult<Entry> result) {
                            assertTrue(result.succeeded());
                            assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                          }
                        });
                        test2.getLog().entry(2, new Handler<AsyncResult<Entry>>() {
                          @Override
                          public void handle(AsyncResult<Entry> result) {
                            assertTrue(result.succeeded());
                            assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                          }
                        });
                        test2.getLog().entry(4, new Handler<AsyncResult<Entry>>() {
                          @Override
                          public void handle(AsyncResult<Entry> result) {
                            assertTrue(result.succeeded());
                            assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                          }
                        });
                        test3.getLog().entry(2, new Handler<AsyncResult<Entry>>() {
                          @Override
                          public void handle(AsyncResult<Entry> result) {
                            assertTrue(result.succeeded());
                            assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                          }
                        });
                        test3.getLog().entry(4, new Handler<AsyncResult<Entry>>() {
                          @Override
                          public void handle(AsyncResult<Entry> result) {
                            assertTrue(result.succeeded());
                            assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                            assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                          }
                        });
                        config.removeMember("test.4");
                        vertx.setTimer(1000, new Handler<Long>() {
                          @Override
                          public void handle(Long event) {
                            test1.getLog().entry(5, new Handler<AsyncResult<Entry>>() {
                              @Override
                              public void handle(AsyncResult<Entry> result) {
                                assertTrue(result.succeeded());
                                assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                              }
                            });
                            test1.getLog().entry(6, new Handler<AsyncResult<Entry>>() {
                              @Override
                              public void handle(AsyncResult<Entry> result) {
                                assertTrue(result.succeeded());
                                assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                                assertFalse(((ConfigurationEntry) result.result()).members().contains("test.4"));
                              }
                            });
                            test2.getLog().entry(5, new Handler<AsyncResult<Entry>>() {
                              @Override
                              public void handle(AsyncResult<Entry> result) {
                                assertTrue(result.succeeded());
                                assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                              }
                            });
                            test2.getLog().entry(6, new Handler<AsyncResult<Entry>>() {
                              @Override
                              public void handle(AsyncResult<Entry> result) {
                                assertTrue(result.succeeded());
                                assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                                assertFalse(((ConfigurationEntry) result.result()).members().contains("test.4"));
                              }
                            });
                            test3.getLog().entry(5, new Handler<AsyncResult<Entry>>() {
                              @Override
                              public void handle(AsyncResult<Entry> result) {
                                assertTrue(result.succeeded());
                                assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.4"));
                              }
                            });
                            test3.getLog().entry(6, new Handler<AsyncResult<Entry>>() {
                              @Override
                              public void handle(AsyncResult<Entry> result) {
                                assertTrue(result.succeeded());
                                assertEquals(Entry.Type.CONFIGURATION, result.result().type());
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.1"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.2"));
                                assertTrue(((ConfigurationEntry) result.result()).members().contains("test.3"));
                                assertFalse(((ConfigurationEntry) result.result()).members().contains("test.4"));
                                testComplete();
                              }
                            });
                          }
                        });
                      }
                    });
                  }
                });
              }
            });
          }
        });
      }
    });
  }

}
