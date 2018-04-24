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
package io.atomix.rest.resources;

import io.atomix.cluster.ClusterEvent;
import io.atomix.cluster.ClusterEvent.Type;
import io.atomix.cluster.ClusterEventListener;
import io.atomix.cluster.ClusterMembershipService;
import io.atomix.cluster.Member;
import io.atomix.cluster.MemberId;
import io.atomix.core.utils.EventLog;
import io.atomix.core.utils.EventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cluster resource.
 */
@Path("/v1/cluster")
public class ClusterResource extends AbstractRestResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterResource.class);

  @GET
  @Path("/node")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getNode(@Context ClusterMembershipService clusterMembershipService) {
    return Response.ok(new NodeInfo(clusterMembershipService.getLocalMember())).build();
  }

  @GET
  @Path("/nodes")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getNodes(@Context ClusterMembershipService clusterMembershipService) {
    return Response.ok(clusterMembershipService.getMembers().stream().map(NodeInfo::new).collect(Collectors.toList())).build();
  }

  @GET
  @Path("/nodes/{node}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getNodeInfo(@PathParam("node") String nodeId, @Context ClusterMembershipService clusterMembershipService) {
    Member member = clusterMembershipService.getMember(MemberId.from(nodeId));
    if (member == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(new NodeInfo(member)).build();
  }

  @GET
  @Path("/events")
  @Produces(MediaType.APPLICATION_JSON)
  public void getEvent(@Context ClusterMembershipService clusterMembershipService, @Context EventManager events, @Suspended AsyncResponse response) {
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.getOrCreateEventLog(ClusterResource.class, "", l -> e -> l.addEvent(e));
    if (eventLog.open()) {
      clusterMembershipService.addListener(eventLog.listener());
    }

    eventLog.nextEvent().whenComplete((result, error) -> {
      if (error == null) {
        response.resume(Response.ok(new NodeEvent(result.subject().id(), result.type())));
      } else {
        LOGGER.warn("{}", error);
        response.resume(Response.serverError().build());
      }
    });
  }

  @POST
  @Path("/events")
  @Produces(MediaType.APPLICATION_JSON)
  public Response addListener(@Context ClusterMembershipService clusterMembershipService, @Context EventManager events) {
    String listenerId = UUID.randomUUID().toString();
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.getOrCreateEventLog(ClusterResource.class, listenerId, l -> e -> l.addEvent(e));
    if (eventLog.open()) {
      clusterMembershipService.addListener(eventLog.listener());
    }
    return Response.ok(listenerId).build();
  }

  @GET
  @Path("/events/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public void getEvent(@PathParam("id") String listenerId, @Context ClusterMembershipService clusterMembershipService, @Context EventManager events, @Suspended AsyncResponse response) {
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.getEventLog(ClusterResource.class, listenerId);
    if (eventLog == null) {
      response.resume(Response.status(Status.NOT_FOUND).build());
      return;
    }

    eventLog.nextEvent().whenComplete((result, error) -> {
      if (error == null) {
        response.resume(Response.ok(new NodeEvent(result.subject().id(), result.type())));
      } else {
        LOGGER.warn("{}", error);
        response.resume(Response.serverError().build());
      }
    });
  }

  @DELETE
  @Path("/events/{id}")
  public void removeListener(@PathParam("id") String listenerId, @Context ClusterMembershipService clusterMembershipService, @Context EventManager events) {
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.removeEventLog(ClusterResource.class, listenerId);
    if (eventLog != null && eventLog.close()) {
      clusterMembershipService.removeListener(eventLog.listener());
    }
  }

  @GET
  @Path("/nodes/{node}/events")
  @Produces(MediaType.APPLICATION_JSON)
  public void getNodeEvent(@PathParam("node") String nodeId, @Context ClusterMembershipService clusterMembershipService, @Context EventManager events, @Suspended AsyncResponse response) {
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.getOrCreateEventLog(ClusterResource.class, nodeId, l -> e -> {
      if (e.subject().id().id().equals(nodeId)) {
        l.addEvent(e);
      }
    });
    if (eventLog.open()) {
      clusterMembershipService.addListener(eventLog.listener());
    }

    eventLog.nextEvent().whenComplete((result, error) -> {
      if (error == null) {
        response.resume(Response.ok(new NodeEvent(result.subject().id(), result.type())));
      } else {
        LOGGER.warn("{}", error);
        response.resume(Response.serverError().build());
      }
    });
  }

  @POST
  @Path("/nodes/{node}/events")
  @Produces(MediaType.APPLICATION_JSON)
  public Response addNodeListener(@PathParam("node") String nodeId, @Context ClusterMembershipService clusterMembershipService, @Context EventManager events) {
    String id = UUID.randomUUID().toString();
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.getOrCreateEventLog(ClusterResource.class, getNodeListener(nodeId, id), l -> e -> {
      if (e.subject().id().id().equals(nodeId)) {
        l.addEvent(e);
      }
    });
    if (eventLog.open()) {
      clusterMembershipService.addListener(eventLog.listener());
    }
    return Response.ok(id).build();
  }

  @GET
  @Path("/nodes/{node}/events/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public void getNodeEvent(@PathParam("node") String nodeId, @PathParam("id") String listenerId, @Context ClusterMembershipService clusterMembershipService, @Context EventManager events, @Suspended AsyncResponse response) {
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.getEventLog(ClusterResource.class, getNodeListener(nodeId, listenerId));
    if (eventLog == null) {
      response.resume(Response.status(Status.NOT_FOUND).build());
      return;
    }

    eventLog.nextEvent().whenComplete((result, error) -> {
      if (error == null) {
        response.resume(Response.ok(new NodeEvent(result.subject().id(), result.type())));
      } else {
        LOGGER.warn("{}", error);
        response.resume(Response.serverError().build());
      }
    });
  }

  @DELETE
  @Path("/nodes/{node}/events/{id}")
  public void removeNodeListener(@PathParam("node") String nodeId, @PathParam("id") String listenerId, @Context ClusterMembershipService clusterMembershipService, @Context EventManager events) {
    EventLog<ClusterEventListener, ClusterEvent> eventLog = events.removeEventLog(ClusterResource.class, getNodeListener(nodeId, listenerId));
    if (eventLog != null && eventLog.close()) {
      clusterMembershipService.removeListener(eventLog.listener());
    }
  }

  private static String getNodeListener(String nodeId, String id) {
    return String.format("%s-%s", nodeId, id);
  }

  /**
   * Node information.
   */
  static class NodeInfo {
    private final Member member;

    NodeInfo(Member member) {
      this.member = member;
    }

    public String getId() {
      return member.id().id();
    }

    public String getHost() {
      return member.address().host();
    }

    public int getPort() {
      return member.address().port();
    }

    public Member.Type getType() {
      return member.type();
    }

    public Member.State getStatus() {
      return member.getState();
    }
  }

  /**
   * Node event.
   */
  static class NodeEvent {
    private final MemberId memberId;
    private final ClusterEvent.Type type;

    public NodeEvent(MemberId memberId, Type type) {
      this.memberId = memberId;
      this.type = type;
    }

    public String getId() {
      return memberId.id();
    }

    public Type getType() {
      return type;
    }
  }
}
