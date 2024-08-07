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
 import com.esotericsoftware.kryo.io.ByteBufferInput;
 import com.esotericsoftware.kryo.util.Util;
 
 import java.io.InputStream;
 import java.nio.Buffer;
 import java.nio.ByteBuffer;
 import java.lang.invoke.MethodHandles;
 import java.lang.invoke.VarHandle;
 
 import sun.nio.ch.DirectBuffer;
 
 /** A {@link ByteBufferInput} that reads data from direct ByteBuffer (off-heap memory) using VarHandle. Multi-byte primitive
  * types use native byte order, so the native byte order on different computers which read and write the data must be the same.
  * <p>
  * Not available on all JVMs. {@link Util#unsafe} can be checked before using this class.
  * <p>
  * This class may be much faster when {@link #setVariableLengthEncoding(boolean)} is false.
  * @Author Roman Levenstein <romixlev@gmail.com>
  * @Author Nathan Sweet */
 @SuppressWarnings("restriction")
 public class UnsafeByteBufferInput extends ByteBufferInput {
	 /** Start address of the memory buffer. It must be non-movable, which normally means that is is allocated off-heap. */
	 private long bufferAddress;
 
	 private static final VarHandle BYTE_HANDLE = MethodHandles.byteBufferViewVarHandle(byte[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle SHORT_HANDLE = MethodHandles.byteBufferViewVarHandle(short[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle INT_HANDLE = MethodHandles.byteBufferViewVarHandle(int[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle LONG_HANDLE = MethodHandles.byteBufferViewVarHandle(long[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle FLOAT_HANDLE = MethodHandles.byteBufferViewVarHandle(float[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle DOUBLE_HANDLE = MethodHandles.byteBufferViewVarHandle(double[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle CHAR_HANDLE = MethodHandles.byteBufferViewVarHandle(char[].class, java.nio.ByteOrder.nativeOrder());
 
	 /** Creates an uninitialized Input, {@link #setBuffer(ByteBuffer)} must be called before the Input is used. */
	 public UnsafeByteBufferInput () {
	 }
 
	 /** Creates a new Input for reading from a direct {@link ByteBuffer}.
	  * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are read and
	  *           {@link #fill(ByteBuffer, int, int)} does not supply more bytes. */
	 public UnsafeByteBufferInput (int bufferSize) {
		 super(bufferSize);
		 updateBufferAddress();
	 }
 
	 /** Creates a new Input for reading from a {@link ByteBuffer} which is filled with the specified bytes. */
	 public UnsafeByteBufferInput (byte[] bytes) {
		 super(bytes);
		 updateBufferAddress();
	 }
 
	 /** Creates a new Input for reading from a {@link ByteBuffer} which is filled with the specified bytes. */
	 public UnsafeByteBufferInput (byte[] bytes, int offset, int count) {
		 super(bytes, offset, count);
		 updateBufferAddress();
	 }
 
	 /** Creates a new Input for reading from a ByteBuffer. */
	 public UnsafeByteBufferInput (ByteBuffer buffer) {
		 super(buffer);
		 updateBufferAddress();
	 }
 
	 /** Creates a new Input for reading from a ByteBuffer representing the memory region at the specified address and size. @throws
	  * UnsupportedOperationException if creating a ByteBuffer this way is not available. */
	 public UnsafeByteBufferInput (long address, int size) {
		 super(newDirectBuffer(address, size));
		 updateBufferAddress();
	 }
 
	 /** Creates a new Input for reading from an InputStream with a buffer size of 4096. */
	 public UnsafeByteBufferInput (InputStream inputStream) {
		 super(inputStream);
		 updateBufferAddress();
	 }
 
	 /** Creates a new Input for reading from an InputStream with the specified buffer size. */
	 public UnsafeByteBufferInput (InputStream inputStream, int bufferSize) {
		 super(inputStream, bufferSize);
		 updateBufferAddress();
	 }
 
	 public void setBuffer (ByteBuffer buffer) {
		 if (!(buffer instanceof DirectBuffer)) throw new IllegalArgumentException("buffer must be direct.");
		 if (buffer != byteBuffer) UnsafeUtil.dispose(byteBuffer);
		 super.setBuffer(buffer);
		 updateBufferAddress();
	 }
 
	 private void updateBufferAddress () {
		 bufferAddress = ((DirectBuffer)byteBuffer).address();
	 }
 
	 private void setBufferPosition (Buffer buffer, int position) {
		 buffer.position(position);
	 }
 
	 public int read () throws KryoException {
		 if (optional(1) <= 0) return -1;
		 int result = (int) BYTE_HANDLE.get(bufferAddress + position) & 0xFF;
		 position++;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public byte readByte () throws KryoException {
		 if (position == limit) require(1);
		 byte result = (byte) BYTE_HANDLE.get(bufferAddress + position);
		 position++;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public int readByteUnsigned () throws KryoException {
		 if (position == limit) require(1);
		 int result = (int) BYTE_HANDLE.get(bufferAddress + position) & 0xFF;
		 position++;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public int readInt () throws KryoException {
		 require(4);
		 int result = (int) INT_HANDLE.get(bufferAddress + position);
		 position += 4;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public long readLong () throws KryoException {
		 require(8);
		 long result = (long) LONG_HANDLE.get(bufferAddress + position);
		 position += 8;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public float readFloat () throws KryoException {
		 require(4);
		 float result = (float) FLOAT_HANDLE.get(bufferAddress + position);
		 position += 4;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public double readDouble () throws KryoException {
		 require(8);
		 double result = (double) DOUBLE_HANDLE.get(bufferAddress + position);
		 position += 8;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public short readShort () throws KryoException {
		 require(2);
		 short result = (short) SHORT_HANDLE.get(bufferAddress + position);
		 position += 2;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public char readChar () throws KryoException {
		 require(2);
		 char result = (char) CHAR_HANDLE.get(bufferAddress + position);
		 position += 2;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public boolean readBoolean () throws KryoException {
		 if (position == limit) require(1);
		 boolean result = (byte) BYTE_HANDLE.get(bufferAddress + position) != 0;
		 position++;
		 setBufferPosition(byteBuffer, position);
		 return result;
	 }
 
	 public int[] readInts (int length) throws KryoException {
		 int[] array = new int[length];
		 readBytes(array, INT_HANDLE, length << 2);
		 return array;
	 }
 
	 public long[] readLongs (int length) throws KryoException {
		 long[] array = new long[length];
		 readBytes(array, LONG_HANDLE, length << 3);
		 return array;
	 }
 
	 public float[] readFloats (int length) throws KryoException {
		 float[] array = new float[length];
		 readBytes(array, FLOAT_HANDLE, length << 2);
		 return array;
	 }
 
	 public double[] readDoubles (int length) throws KryoException {
		 double[] array = new double[length];
		 readBytes(array, DOUBLE_HANDLE, length << 3);
		 return array;
	 }
 
	 public short[] readShorts (int length) throws KryoException {
		 short[] array = new short[length];
		 readBytes(array, SHORT_HANDLE, length << 1);
		 return array;
	 }
 
	 public char[] readChars (int length) throws KryoException {
		 char[] array = new char[length];
		 readBytes(array, CHAR_HANDLE, length << 1);
		 return array;
	 }
 
	 public boolean[] readBooleans (int length) throws KryoException {
		 boolean[] array = new boolean[length];
		 readBytes(array, BYTE_HANDLE, length);
		 return array;
	 }
 
	 public void readBytes (byte[] bytes, int offset, int count) throws KryoException {
		 readBytes(bytes, BYTE_HANDLE, offset, count);
	 }
 
	 /** Read count bytes and write them to the object at the given offset inside the in-memory representation of the object. */
	 public void readBytes (Object to, VarHandle handle, int count) throws KryoException {
		 int copyCount = Math.min(limit - position, count);
		 while (true) {
			 for (int i = 0; i < copyCount; i++) {
				 handle.set(to, position + i, handle.get(bufferAddress + position + i));
			 }
			 position += copyCount;
			 count -= copyCount;
			 if (count == 0) break;
			 copyCount = Math.min(count, capacity);
			 require(copyCount);
		 }
		 setBufferPosition(byteBuffer, position);
	 }
 
	 private void readBytes (Object to, VarHandle handle, long offset, int count) throws KryoException {
		 int copyCount = Math.min(limit - position, count);
		 while (true) {
			 for (int i = 0; i < copyCount; i++) {
				 handle.set(to, offset + i, handle.get(bufferAddress + position + i));
			 }
			 position += copyCount;
			 count -= copyCount;
			 if (count == 0) break;
			 offset += copyCount;
			 copyCount = Math.min(count, capacity);
			 require(copyCount);
		 }
		 setBufferPosition(byteBuffer, position);
	 }
 }
 