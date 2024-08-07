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
 import com.esotericsoftware.kryo.io.Input;
 import com.esotericsoftware.kryo.util.Util;
 
 import java.io.InputStream;
 import java.lang.invoke.MethodHandles;
 import java.lang.invoke.VarHandle;
 
 /** An {@link Input} that reads data from a byte[] using sun.misc.Unsafe. Multi-byte primitive types use native byte order, so the
  * native byte order on different computers which read and write the data must be the same.
  * <p>
  * Not available on all JVMs. {@link Util#unsafe} can be checked before using this class.
  * <p>
  * This class may be much faster when {@link #setVariableLengthEncoding(boolean)} is false.
  * @author Roman Levenstein <romixlev@gmail.com>
  * @author Nathan Sweet */
 @SuppressWarnings("restriction")
 public class UnsafeInput extends Input {
	 private static final VarHandle BYTE_HANDLE = MethodHandles.byteArrayViewVarHandle(byte[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle SHORT_HANDLE = MethodHandles.byteArrayViewVarHandle(short[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle INT_HANDLE = MethodHandles.byteArrayViewVarHandle(int[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle LONG_HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle FLOAT_HANDLE = MethodHandles.byteArrayViewVarHandle(float[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle DOUBLE_HANDLE = MethodHandles.byteArrayViewVarHandle(double[].class, java.nio.ByteOrder.nativeOrder());
	 private static final VarHandle CHAR_HANDLE = MethodHandles.byteArrayViewVarHandle(char[].class, java.nio.ByteOrder.nativeOrder());
 
	 public UnsafeInput() {
	 }
 
	 public UnsafeInput(int bufferSize) {
		 super(bufferSize);
	 }
 
	 public UnsafeInput(byte[] buffer) {
		 super(buffer);
	 }
 
	 public UnsafeInput(byte[] buffer, int offset, int count) {
		 super(buffer, offset, count);
	 }
 
	 public UnsafeInput(InputStream inputStream) {
		 super(inputStream);
	 }
 
	 public UnsafeInput(InputStream inputStream, int bufferSize) {
		 super(inputStream, bufferSize);
	 }
 
	 public int read() throws KryoException {
		 if (optional(1) <= 0) return -1;
		 return (int) BYTE_HANDLE.get(buffer, position++) & 0xFF;
	 }
 
	 public byte readByte() throws KryoException {
		 if (position == limit) require(1);
		 return (byte) BYTE_HANDLE.get(buffer, position++);
	 }
 
	 public int readByteUnsigned() throws KryoException {
		 if (position == limit) require(1);
		 return (int) BYTE_HANDLE.get(buffer, position++) & 0xFF;
	 }
 
	 public int readInt() throws KryoException {
		 require(4);
		 int result = (int) INT_HANDLE.get(buffer, position);
		 position += 4;
		 return result;
	 }
 
	 public long readLong() throws KryoException {
		 require(8);
		 long result = (long) LONG_HANDLE.get(buffer, position);
		 position += 8;
		 return result;
	 }
 
	 public float readFloat() throws KryoException {
		 require(4);
		 float result = (float) FLOAT_HANDLE.get(buffer, position);
		 position += 4;
		 return result;
	 }
 
	 public double readDouble() throws KryoException {
		 require(8);
		 double result = (double) DOUBLE_HANDLE.get(buffer, position);
		 position += 8;
		 return result;
	 }
 
	 public short readShort() throws KryoException {
		 require(2);
		 short result = (short) SHORT_HANDLE.get(buffer, position);
		 position += 2;
		 return result;
	 }
 
	 public char readChar() throws KryoException {
		 require(2);
		 char result = (char) CHAR_HANDLE.get(buffer, position);
		 position += 2;
		 return result;
	 }
 
	 public boolean readBoolean() throws KryoException {
		 if (position == limit) require(1);
		 return (byte) BYTE_HANDLE.get(buffer, position++) != 0;
	 }
 
	 public int[] readInts(int length) throws KryoException {
		 int[] array = new int[length];
		 for (int i = 0; i < length; i++) {
			 array[i] = readInt();
		 }
		 return array;
	 }
 
	 public long[] readLongs(int length) throws KryoException {
		 long[] array = new long[length];
		 for (int i = 0; i < length; i++) {
			 array[i] = readLong();
		 }
		 return array;
	 }
 
	 public float[] readFloats(int length) throws KryoException {
		 float[] array = new float[length];
		 for (int i = 0; i < length; i++) {
			 array[i] = readFloat();
		 }
		 return array;
	 }
 
	 public double[] readDoubles(int length) throws KryoException {
		 double[] array = new double[length];
		 for (int i = 0; i < length; i++) {
			 array[i] = readDouble();
		 }
		 return array;
	 }
 
	 public short[] readShorts(int length) throws KryoException {
		 short[] array = new short[length];
		 for (int i = 0; i < length; i++) {
			 array[i] = readShort();
		 }
		 return array;
	 }
 
	 public char[] readChars(int length) throws KryoException {
		 char[] array = new char[length];
		 for (int i = 0; i < length; i++) {
			 array[i] = readChar();
		 }
		 return array;
	 }
 
	 public boolean[] readBooleans(int length) throws KryoException {
		 boolean[] array = new boolean[length];
		 for (int i = 0; i < length; i++) {
			 array[i] = readBoolean();
		 }
		 return array;
	 }
 
	 public void readBytes(byte[] bytes, int offset, int count) throws KryoException {
		 if (position + count > limit) require(count);
		 System.arraycopy(buffer, position, bytes, offset, count);
		 position += count;
	 }
 }
 