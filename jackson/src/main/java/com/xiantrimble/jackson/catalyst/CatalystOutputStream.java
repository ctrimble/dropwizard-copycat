package com.xiantrimble.jackson.catalyst;

import java.io.IOException;
import java.io.OutputStream;

import io.atomix.catalyst.buffer.BufferOutput;

public class CatalystOutputStream extends OutputStream {

	private BufferOutput<?> out;

	public CatalystOutputStream( BufferOutput<?> out ) {
		this.out = out;
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}


	@Override
	public void write(int b) throws IOException {
		out.writeByte(b);
	}
}
