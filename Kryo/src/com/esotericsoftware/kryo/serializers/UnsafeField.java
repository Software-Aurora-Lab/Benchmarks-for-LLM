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

 package com.esotericsoftware.kryo.serializers;

 import static com.esotericsoftware.kryo.unsafe.UnsafeUtil.*;
 
 import com.esotericsoftware.kryo.KryoException;
 import com.esotericsoftware.kryo.io.Input;
 import com.esotericsoftware.kryo.io.Output;
 import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
 import com.esotericsoftware.kryo.util.Generics.GenericType;
 
 import java.lang.invoke.MethodHandles;
 import java.lang.invoke.VarHandle;
 import java.lang.reflect.Field;
 
 /** Read and write a non-primitive field using VarHandle.
  * @Author Nathan Sweet */
 @SuppressWarnings("restriction")
 class UnsafeField extends ReflectField {
	 private VarHandle handle;
 
	 public UnsafeField(Field field, FieldSerializer serializer, GenericType genericType) {
		 super(field, serializer, genericType);
		 try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
            handle = lookup.unreflectVarHandle(field);
		 } catch (IllegalAccessException e) {
			 throw new RuntimeException(e);
		 }
	 }
 
	 public Object get(Object object) throws IllegalAccessException {
		 return handle.get(object);
	 }
 
	 public void set(Object object, Object value) throws IllegalAccessException {
		 handle.set(object, value);
	 }
 
	 public void copy(Object original, Object copy) {
		 try {
			 handle.set(copy, fieldSerializer.kryo.copy(handle.get(original)));
		 } catch (KryoException ex) {
			 ex.addTrace(this + " (" + fieldSerializer.type.getName() + ")");
			 throw ex;
		 } catch (Throwable t) {
			 KryoException ex = new KryoException(t);
			 ex.addTrace(this + " (" + fieldSerializer.type.getName() + ")");
			 throw ex;
		 }
	 }
 
	 static final class IntUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public IntUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
				handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 if (varEncoding)
				 output.writeVarInt((int) handle.get(object), false);
			 else
				 output.writeInt((int) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 if (varEncoding)
				 handle.set(object, input.readVarInt(false));
			 else
				 handle.set(object, input.readInt());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class FloatUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public FloatUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
				handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 output.writeFloat((float) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 handle.set(object, input.readFloat());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class ShortUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public ShortUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
				handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 output.writeShort((short) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 handle.set(object, input.readShort());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class ByteUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public ByteUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
				handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 output.writeByte((byte) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 handle.set(object, input.readByte());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class BooleanUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public BooleanUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
				handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 output.writeBoolean((boolean) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 handle.set(object, input.readBoolean());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class CharUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public CharUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
            	handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 output.writeChar((char) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 handle.set(object, input.readChar());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class LongUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public LongUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
            	handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 if (varEncoding)
				 output.writeVarLong((long) handle.get(object), false);
			 else
				 output.writeLong((long) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 if (varEncoding)
				 handle.set(object, input.readVarLong(false));
			 else
				 handle.set(object, input.readLong());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class DoubleUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public DoubleUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
				handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 output.writeDouble((double) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 handle.set(object, input.readDouble());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 
	 static final class StringUnsafeField extends CachedField {
		 private VarHandle handle;
 
		 public StringUnsafeField(Field field) {
			 super(field);
			 try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
				handle = lookup.unreflectVarHandle(field);
			 } catch (IllegalAccessException e) {
				 throw new RuntimeException(e);
			 }
		 }
 
		 public void write(Output output, Object object) {
			 output.writeString((String) handle.get(object));
		 }
 
		 public void read(Input input, Object object) {
			 handle.set(object, input.readString());
		 }
 
		 public void copy(Object original, Object copy) {
			 handle.set(copy, handle.get(original));
		 }
	 }
 }
 