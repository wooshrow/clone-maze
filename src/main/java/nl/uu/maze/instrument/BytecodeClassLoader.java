package nl.uu.maze.instrument;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom class loader that allows you to define classes from byte arrays.
 * These classes are added and managed in {@link #classes}. When asked to
 * find a class, this loader will first look in its managed {@link classes}.
 * If the asked classes is not found there, the loader falls back to Java
 * default loader.
 */
public class BytecodeClassLoader extends ClassLoader {
	
	
    private final Map<String, Class<?>> classes = new HashMap<>();

    /**
     * Find a class with the given name, among the classes managed by this
     * loader. These are kept in {@link #classes}. If it is not found there,
     * this loader will fall back to Java default class loader.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = classes.get(name);
        return clazz != null ? clazz : super.findClass(name);
    }

    /**
     * Add a class from a byte array.
     *
     * @param name       The name of the class
     * @param classBytes The byte array containing the class data
     */
    public void addClass(String name, byte[] classBytes) {
        if (classes.containsKey(name)) {
            classes.get(name);
            return;
        }
        Class<?> clazz = defineClass(name, classBytes, 0, classBytes.length);
        resolveClass(clazz);
        classes.put(name, clazz);
    }
}
