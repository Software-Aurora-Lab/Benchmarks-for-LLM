package unsafeString;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class DirectCharArray {

	private Unsafe unsafe;

	private final static long CHAR_SIZE_IN_BYTES = 2;

	private long startIndex;
	private long size;
	
	private static class DirectCharArrayHolder {
		public static final DirectCharArray INSTANCE = new DirectCharArray();
	}
	
	public static DirectCharArray getInstance() {
		return DirectCharArrayHolder.INSTANCE;
	}
	
	private DirectCharArray() {

		Field theUnsafe = null;
		try {
			theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			unsafe = (Unsafe) theUnsafe.get(null);
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}


	}

	public void allocate(long sizeParam) {
		
		sizeParam += 1;
		
		if(size >= sizeParam) return;

		if (sizeParam > size) destroy();
		
		System.out.println("allocate");
		
		this.size = sizeParam * CHAR_SIZE_IN_BYTES;

		startIndex = unsafe.allocateMemory(sizeParam * CHAR_SIZE_IN_BYTES);
		unsafe.setMemory(startIndex, sizeParam * CHAR_SIZE_IN_BYTES, (byte) 0);		
		
	}
	
	public void intern(String str) {
		
		Field valueField = null;
		char[] value = null;
		try {
			valueField = str.getClass().getDeclaredField("value");
			valueField.setAccessible(true);
			value = (char[]) valueField.get(str);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		
		int base = unsafe.arrayBaseOffset(char[].class);
		
		//long objectFieldOffset = unsafe.objectFieldOffset(value);
		
		unsafe.copyMemory(value, base, null, startIndex, size);
	}
	
	public void set(long index, char value) {
		unsafe.putChar(index(index), value);
	}

	public char get(long index) {
		return unsafe.getChar(index(index));
	}

	public char[] get(long ini, long end) {
		int lenght = (int)(end - ini);
		char[] ret = new char[lenght - 1];
		for(int pos = (int) ini, idx = 0; pos < end - 1; pos++, idx++) {
			ret[idx] = unsafe.getChar(index(pos));
		}
		return ret;
	}

	private long index(long offset) {
		return startIndex + offset * CHAR_SIZE_IN_BYTES;
	}

	public void destroy() {
		unsafe.freeMemory(startIndex);
	}
}
