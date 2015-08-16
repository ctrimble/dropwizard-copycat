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
package com.xiantrimble.dropwizard.copycat.example;

import net.kuujo.copycat.io.BufferInput;
import net.kuujo.copycat.io.BufferOutput;
import net.kuujo.copycat.io.serializer.CopycatSerializable;
import net.kuujo.copycat.io.serializer.Serializer;
import net.kuujo.copycat.raft.Command;
import net.kuujo.copycat.util.BuilderPool;

public class ComputationCommands {
  /**
   * Base for computation commands.
   */
  public static abstract class ComputationCommand<V> implements Command<V>, CopycatSerializable {

    /**
     * Base map command builder.
     */
    public static abstract class Builder<T extends Builder<T, U, V>, U extends ComputationCommand<V>, V>
        extends Command.Builder<T, U, V> {
      protected Builder(BuilderPool<T, U> pool) {
        super(pool);
      }
    }
  }

  public static abstract class SegmentCommand<V> extends ComputationCommand<V> {
    protected long index;

    public long getIndex() {
      return index;
    }

    @Override
    public void writeObject(BufferOutput buffer, Serializer serializer) {
      buffer.writeLong(index);
    }

    @Override
    public void readObject(BufferInput buffer, Serializer serializer) {
      index = buffer.readLong();
    }
  }
}
