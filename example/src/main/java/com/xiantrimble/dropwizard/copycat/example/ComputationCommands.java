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

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.copycat.client.Command;

public class ComputationCommands {
  /**
   * Base for computation commands.
   */
  public static abstract class ComputationCommand<V> implements Command<V>, CatalystSerializable {

	private static final long serialVersionUID = 1L;
  }

  public static abstract class SegmentCommand<V> extends ComputationCommand<V> {

	private static final long serialVersionUID = 1L;
	protected long index;

    public long getIndex() {
      return index;
    }

    @Override
    public void writeObject(@SuppressWarnings("rawtypes") BufferOutput buffer, Serializer serializer) {
      buffer.writeLong(index);
    }

	@Override
    public void readObject(@SuppressWarnings("rawtypes") BufferInput buffer, Serializer serializer) {
      index = buffer.readLong();
    }
  }
}
