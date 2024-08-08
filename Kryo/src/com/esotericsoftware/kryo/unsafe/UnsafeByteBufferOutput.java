/* Copyright (c) 2008-2023, Nathan Sweet
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.unsafe;

import static com.esotericsoftware.kryo.unsafe.UnsafeUtil.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.util.Util;

import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import sun.nio.ch.DirectBuffer;

/** A {@link ByteBufferOutput} that writes data to a direct ByteBuffer (off-heap memory) using sun.misc.Unsafe. Multi-byte
 * primitive types use native byte order, so the native byte order on different computers which read and write the data must be
 * the same.
 * <p>
 * Not available on all JVMs. {@link Util#unsafe} can be checked before using this class.
 * <p>
 * This class may be much faster when {@link #setVariableLengthEncoding(boolean)} is false.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
@SuppressWarnings("restriction")
public class UnsafeByteBufferOutput extends ByteBufferOutput {
	/** Start address of the memory buffer. It must be non-movable, which normally means that is is allocated off-heap. */
	private long bufferAddress;

	/** Creates an uninitialized Output, {@link #setBuffer(ByteBuffer)} must be called before the Output is used. */
	public UnsafeByteBufferOutput () {
	}

	/** Creates a new Output for writing to a direct {@link ByteBuffer}.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are written and {@link #flush()}
	 *           does not empty the buffer. */
	public UnsafeByteBufferOutput (int bufferSize) {
		super(bufferSize);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to a direct ByteBuffer.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public UnsafeByteBufferOutput (int bufferSize, int maxBufferSize) {
		super(bufferSize, maxBufferSize);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public UnsafeByteBufferOutput (OutputStream outputStream) {
		super(outputStream);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to an OutputStream with the specified buffer size. */
	public UnsafeByteBufferOutput (OutputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to a ByteBuffer representing the memory region at the specified address and size. @throws
	 * UnsupportedOperationException if creating a ByteBuffer this way is not available. */
	public UnsafeByteBufferOutput (long address, int size) {
		super(newDirectBuffer(address, size));
		updateBufferAddress();
	}

	public void setBuffer (ByteBuffer buffer, int maxBufferSize) {
		if (!(buffer instanceof DirectBuffer)) throw new IllegalArgumentException("buffer must be direct.");
		if (buffer != byteBuffer) UnsafeUtil.dispose(byteBuffer);
		super.setBuffer(buffer, maxBufferSize);
		updateBufferAddress();
	}

	private void updateBufferAddress () {
		bufferAddress = ((DirectBuffer)byteBuffer).address();
	}

	protected boolean require (int required) throws KryoException {
		ByteBuffer oldBuffer = byteBuffer;
		boolean result = super.require(required);
		if (byteBuffer != oldBuffer) {
			UnsafeUtil.dispose(oldBuffer);
			updateBufferAddress();
		}
		return result;
	}

	/** Releases the byte buffer immediately, rather than waiting for GC. This output can no longer be used until a new byte buffer
	 * is set. */
	public void dispose () {
		UnsafeUtil.dispose(byteBuffer);
		byteBuffer = null;
		bufferAddress = 0;
	}

	private void setBufferPosition (Buffer buffer, int position) {
		buffer.position(position);
	}
}