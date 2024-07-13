import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.HashSet;
import sun.misc.Unsafe;
import java.lang.reflect.Modifier;

public class sizeOfTest {

    public static void main(String[] args) throws Throwable {
        MyObject myObject = new MyObject();
        System.out.println("Size of MyObject instance: " + sizeOf(myObject) + " bytes");
        System.out.println("Size of MyObject instance: " + sizeOfFFM(myObject) + " bytes");
    }

    public static long sizeOf(Object o) {
        // Using Unsafe to get the size
        HashSet<Field> fields = new HashSet<>();
        Class<?> c = o.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) == 0) {
                    fields.add(f);
                }
            }
            c = c.getSuperclass();
        }

        long maxSize = 0;
        for (Field f : fields) {
            long offset = getUnsafe().objectFieldOffset(f);
            if (offset > maxSize) {
                maxSize = offset;
            }
        }

        return ((maxSize / 8) + 1) * 8; // padding
    }

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long sizeOfFFM(Object o) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            // Create a memory layout for the object and its fields
            MemoryLayout layout = MemoryLayout.structLayout(
                    ValueLayout.ADDRESS.withName("str"),
                    ValueLayout.JAVA_BOOLEAN.withName("bool"),
                    MemoryLayout.paddingLayout(3),
                    MemoryLayout.sequenceLayout(5, ValueLayout.JAVA_INT).withName("array"),
                    ValueLayout.JAVA_FLOAT.withName("fl")
            );

            // Allocate memory for the object
            MemorySegment native_struct = arena.allocate(layout);

            // Get the offset using the linker
            Linker linker = Linker.nativeLinker();
            SymbolLookup stdLib = SymbolLookup.libraryLookup("simple test/src/libsizeof.so", arena);
            MemorySegment sizeof_addr = stdLib.find("sizeof_myobject").get();
            FunctionDescriptor offsetof_sig = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);
            MethodHandle offsetof = linker.downcallHandle(sizeof_addr, offsetof_sig);

            return (long) offsetof.invokeExact(native_struct);
        }
    }

    static class MyObject {
        char[] str = {'H', 'e', 'l', 'l', 'o'};
        boolean bool = true;
        int[] array = new int[5];
        float fl = 1.0f;
    }
}
