/**
 * Copyright (C) 2015 Christian Trimble (xiantrimble@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiantrimble.dropwizard.copycat;

import java.util.List;
import net.kuujo.copycat.CopycatServer;
import net.kuujo.copycat.io.storage.Log;
import net.kuujo.copycat.io.storage.StorageLevel;
import net.kuujo.copycat.io.transport.NettyTransport;
import net.kuujo.copycat.raft.Member;
import net.kuujo.copycat.raft.Members;

public class CopycatConfiguration {
  protected int memberId;
  protected List<MemberConfiguration> members;

  public int getMemberId() {
    return memberId;
  }

  public void setMemberId(int memberId) {
    this.memberId = memberId;
  }

  public List<MemberConfiguration> getMembers() {
    return members;
  }

  public void setMembers(List<MemberConfiguration> members) {
    this.members = members;
  }

  public CopycatServer createServer() {
    return CopycatServer.builder()
    		.withMemberId(memberId)
    		.withMembers(members())
            .withTransport(NettyTransport.builder()
            		.withThreads(5)
                    .build())
                  .withLog(Log.builder()
                    .withStorageLevel(StorageLevel.MEMORY)
                    .build())
    		.build();
  }

  public Members members() {
    Members.Builder builder = Members.builder();
    members
        .stream()
        .map(
            m -> Member.builder().withId(m.getId()).withHost(m.getHost()).withPort(m.getPort())
                .build()).forEach(builder::addMember);
    return builder.build();

  }
}
