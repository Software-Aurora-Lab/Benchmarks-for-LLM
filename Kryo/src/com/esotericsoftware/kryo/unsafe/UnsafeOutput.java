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
 import com.esotericsoftware.kryo.io.Output;
 import com.esotericsoftware.kryo.util.Util;
 
 import java.io.OutputStream;
 import java.lang.invoke.MethodHandles;
 import java.lang.invoke.VarHandle;
 
 /** An {@link Output} that writes data using sun.misc.Unsafe. Multi-byte primitive types use native byte order, so the native byte
  * order on different computers which read and write the data must be the same.
  * <p>
  * Not available on all JVMs. {@link Util#unsafe} can be checked before using this class.
  * <p>
  * This class may be much faster when {@link #setVariableLengthEncoding(boolean)} is false.
  * @author Roman Levenstein <romixlev@gmail.com>
  * @author Nathan Sweet */
 @SuppressWarnings("restriction")
 public class UnsafeOutput extends Output {
	 private static final VarHandle BYTE_HANDLE = MethodHandles.byteArrayViewVarHandle(byte[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle SHORT_HANDLE = MethodHandles.byteArrayViewVarHandle(short[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle INT_HANDLE = MethodHandles.byteArrayViewVarHandle(int[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle LONG_HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle FLOAT_HANDLE = MethodHandles.byteArrayViewVarHandle(float[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle DOUBLE_HANDLE = MethodHandles.byteArrayViewVarHandle(double[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle CHAR_HANDLE = MethodHandles.byteArrayViewVarHandle(char[].class, java.nio.ByteOrder.nativeOrder());
 
	 public UnsafeOutput() {
	 }
 
	 public UnsafeOutput(int bufferSize) {
		 this(bufferSize, bufferSize);
	 }
 
	 public UnsafeOutput(int bufferSize, int maxBufferSize) {
		 super(bufferSize, maxBufferSize);
	 }
 
	 public UnsafeOutput(byte[] buffer) {
		 this(buffer, buffer.length);
	 }
 
	 public UnsafeOutput(byte[] buffer, int maxBufferSize) {
		 super(buffer, maxBufferSize);
	 }
 
	 public UnsafeOutput(OutputStream outputStream) {
		 super(outputStream);
	 }
 
	 public UnsafeOutput(OutputStream outputStream, int bufferSize) {
		 super(outputStream, bufferSize);
	 }
 
	 public void write(int value) throws KryoException {
		 if (position == capacity) require(1);
		 BYTE_HANDLE.set(buffer, position++, (byte) value);
	 }
 
	 public void writeByte(byte value) throws KryoException {
		 if (position == capacity) require(1);
		 BYTE_HANDLE.set(buffer, position++, value);
	 }
 
	 public void writeByte(int value) throws KryoException {
		 if (position == capacity) require(1);
		 BYTE_HANDLE.set(buffer, position++, (byte) value);
	 }
 
	 public void writeInt(int value) throws KryoException {
		 require(4);
		 INT_HANDLE.set(buffer, position, value);
		 position += 4;
	 }
 
	 public void writeLong(long value) throws KryoException {
		 require(8);
		 LONG_HANDLE.set(buffer, position, value);
		 position += 8;
	 }
 
	 public void writeFloat(float value) throws KryoException {
		 require(4);
		 FLOAT_HANDLE.set(buffer, position, value);
		 position += 4;
	 }
 
	 public void writeDouble(double value) throws KryoException {
		 require(8);
		 DOUBLE_HANDLE.set(buffer, position, value);
		 position += 8;
	 }
 
	 public void writeShort(int value) throws KryoException {
		 require(2);
		 SHORT_HANDLE.set(buffer, position, (short) value);
		 position += 2;
	 }
 
	 public void writeChar(char value) throws KryoException {
		 require(2);
		 CHAR_HANDLE.set(buffer, position, value);
		 position += 2;
	 }
 
	 public void writeBoolean(boolean value) throws KryoException {
		 if (position == capacity) require(1);
		 BYTE_HANDLE.set(buffer, position++, value ? (byte) 1 : 0);
	 }
 
	 public void writeInts(int[] array, int offset, int count) throws KryoException {
		 for (int i = 0; i < count; i++) {
			 writeInt(array[offset + i]);
		 }
	 }
 
	 public void writeLongs(long[] array, int offset, int count) throws KryoException {
		 for (int i = 0; i < count; i++) {
			 writeLong(array[offset + i]);
		 }
	 }
 
	 public void writeFloats(float[] array, int offset, int count) throws KryoException {
		 for (int i = 0; i < count; i++) {
			 writeFloat(array[offset + i]);
		 }
	 }
 
	 public void writeDoubles(double[] array, int offset, int count) throws KryoException {
		 for (int i = 0; i < count; i++) {
			 writeDouble(array[offset + i]);
		 }
	 }
 
	 public void writeShorts(short[] array, int offset, int count) throws KryoException {
		 for (int i = 0; i < count; i++) {
			 writeShort(array[offset + i]);
		 }
	 }
 
	 public void writeChars(char[] array, int offset, int count) throws KryoException {
		 for (int i = 0; i < count; i++) {
			 writeChar(array[offset + i]);
		 }
	 }
 
	 public void writeBooleans(boolean[] array, int offset, int count) throws KryoException {
		 for (int i = 0; i < count; i++) {
			 writeBoolean(array[offset + i]);
		 }
	 }
 
	 public void writeBytes(byte[] array, int offset, int count) throws KryoException {
		 if (position + count > capacity) require(count);
		 System.arraycopy(array, offset, buffer, position, count);
		 position += count;
	 }
 }
 