package com.xiantrimble.jackson.catalyst;

import java.io.IOException;
import java.io.InputStream;

import io.atomix.catalyst.buffer.BufferInput;

public class CatalystInputStream extends InputStream {
	
	private BufferInput<?> in;

	public CatalystInputStream( BufferInput<?> in ) {
		this.in = in;
	}

	@Override
	public int read() throws IOException {
		return in.readByte();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return (int)(in.remaining() - in.read(b).remaining());
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return (int)(in.remaining() - in.read(b, off, len).remaining());
	}

	@Override
	public long skip(long n) throws IOException {
		if( n < 0 ) return 0L;
		return in.remaining() - in.skip(n).remaining();
	}

	@Override
	public int available() throws IOException {
		return (int)in.remaining();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
