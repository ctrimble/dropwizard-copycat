package com.xiantrimble.dropwizard.copycat.example.resource;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiantrimble.dropwizard.copycat.example.ExampleStateMachine;

import io.atomix.copycat.client.CopycatClient;

/**
 * An example resource that allows management of keys and JSON values.
 */
@Singleton
@Path("keys")
public class StateMachineResource {
  @Inject
  public CopycatClient client;
  
  @GET
  @Produces("application/json")
  public Set<String> getKeys() throws InterruptedException, ExecutionException {
	  return client.submit(new ExampleStateMachine.GetKeys()).get();
  }
  
  @GET
  @Path("{key}")
  @Produces("application/json")
  public JsonNode get( @PathParam("key") String key) throws InterruptedException, ExecutionException {
	  return client.submit(new ExampleStateMachine.GetValue().withKey(key)).get();
  }
  
  @PUT
  @Path("{key}")
  @Consumes("application/json")
  public void set( @PathParam("key") String key, JsonNode value ) throws InterruptedException, ExecutionException {
	  client.submit(new ExampleStateMachine.SetValue().withKey(key).withNode(value)).get();
  }
}
