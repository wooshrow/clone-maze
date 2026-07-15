package nl.uu.maze.execution;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.uu.maze.analysis.JavaAnalyzer;
import nl.uu.maze.execution.concrete.ExecutionResult;
import nl.uu.maze.execution.concrete.ObjectInstantiation;
import nl.uu.maze.execution.symbolic.SymbolicStateValidator;
import nl.uu.maze.util.ObjectUtils;
import nl.uu.maze.util.Z3Sorts;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.PrimitiveType.IntType;
import sootup.core.types.PrimitiveType.LongType;
import sootup.core.types.PrimitiveType.FloatType;
import sootup.core.types.PrimitiveType.DoubleType;
import sootup.core.types.PrimitiveType.BooleanType;

import sootup.core.types.Type;

/**
 * Represents a map of arguments to be passed to a method.
 * Created either by the {@link ObjectInstantiation} randomly or from a Z3 model
 * by the {@link SymbolicStateValidator}.
 * 
 * <p>
 * Arguments are identified based on their names, e.g., "carg0" is the first
 * argument of a constructor, and "marg0" is the first argument of a regular
 * method.
 * These names should be created using the {@link #getSymbolicName
 * getSymbolicName}
 * method.
 * </p>
 */
public class ArgMap {
    private static final Logger logger = LoggerFactory.getLogger(ArgMap.class);
    private static final Z3Sorts sorts = Z3Sorts.getInstance();

    /**
     * Map of "arguments" (though it can contain any variable or object reference
     * present in the program).
     * Primitive values and arrays are represented as their Java native value,
     * object references are represented as {@link ObjectRef},
     * and object instances are represented as {@link ObjectInstance}.
     * It is possible for arrays to be of the Object[] type, even if the actual
     * array is of a more specific type, in which case the array can be converted to
     * the correct type using the {@link #toJava toJava} method.
     */
    private final Map<String, Object> args;
    /**
     * Map of objects converted to the correct Java type.
     */
    private final Map<String, Object> converted = new HashMap<>();
    /**
     * Track of how many times a variable is referenced by an ObjectRef.
     * Lazily instantiated to avoid unnecessary overhead.
     */
    private Map<String, Integer> refCount = null;

    public ArgMap() {
        this.args = new java.util.HashMap<>();
    }

    public void set(String key, Object value) {
        args.put(key, value);
    }

    public Object get(String key) {
        return args.get(key);
    }

    public Object getOrDefault(String key, Object defaultValue) {
        return args.getOrDefault(key, defaultValue);
    }

    public Object getOrNew(String key, Object newValue) {
        if (!args.containsKey(key)) {
            args.put(key, newValue);
            return newValue;
        }
        return args.get(key);
    }

    public boolean containsKey(String key) {
        return args.containsKey(key);
    }

    /**
     * Clear the map of converted values.
     * Useful after using this ArgMap instance in a method/constructor invocation to
     * ensure that the object and/or arrays are not re-used, since they may have
     * been modified by the mehtod/constructor invocation.
     */
    public void resetConverted() {
        converted.clear();
    }

    /**
     * Get an appropriate symbolic name for a parameter based on its index and a
     * prefix to avoid name conflicts between different methods (e.g., constructor
     * and target method).
     */
    public static String getSymbolicName(MethodType type, int index) {
        return type.getPrefix() + "arg" + index;
    }

    public Set<String> getArgsNames() {
    	return args.keySet() ;
    }
    
    @Override
    public String toString() {
        return args.toString();
    }

    /**
     * Follow a (chain of) reference(s) to the actual object.
     * 
     * @param var           The variable to start from
     * @param singleUseOnly Whether to only follow the reference if it is a
     *                      single-use
     *                      reference (i.e., every entry along the chain is
     *                      referenced only once)
     * @return The object the reference points to or {@code null} if set to
     *         single-use only and some reference along the chain is used more than
     *         once
     */
    public Optional<Object> followRef(String var, boolean singleUseOnly) {
        Object value = args.get(var);
        if (value instanceof ObjectRef ref) {
            // If set to singleUseOnly and the ref is used multiple times, or it's a
            // parameter, don't follow the chain
            if (singleUseOnly && (ref.getVar().contains("arg") || getRefCount(ref.getVar()) > 1)) {
                return Optional.empty();
            }
            return followRef(ref.getVar(), singleUseOnly);
        }
        return Optional.ofNullable(value);
    }

    /**
     * Get the number of times a variable is referenced by an ObjectRef.
     */
    public int getRefCount(String var) {
        if (refCount != null) {
            return refCount.getOrDefault(var, 0);
        }

        refCount = new HashMap<>();
        for (Object value : args.values()) {
            if (value instanceof ObjectRef) {
                String ref = ((ObjectRef) value).getVar();
                refCount.put(ref, refCount.getOrDefault(ref, 0) + 1);
            }
        }
        return refCount.getOrDefault(var, 0);
    }

    /**
     * Convert an entry in the ArgMap to the correct Java type.
     * 
     * @param key  The key of the entry
     * @param type The type to convert the entry to
     * @return The converted object
     */
    public Object toJava(String key, Class<?> type) {
    	return toJava(key, args.get(key), type);
    }

    /**
     * Convert an entry in the ArgMap to the correct Java type.
     * Stores the converted object in the converted map to avoid re-converting the
     * same object multiple times and to allow referencing the same object multiple
     * times.
     */
    private Object toJava(String key, Object value, Class<?> type) {
    	
    	//if (value == null) {    		
    	//   	System.out.println(">>> toJava " + key + ", v=" + value + ", ty=" + type.getSimpleName()) ;   		
    	//}
    	//else {
    	//   	System.out.println(">>> toJava " + key + ", v=" + value + ":" + value.getClass().getSimpleName() + ", ty=" + type.getSimpleName()) ;   		
    	//}
        // If already defined from resolving a reference, return that
        if (converted.containsKey(key) && !key.equals("temp")) {
            return converted.get(key);
        }

        if (value == null) {
            converted.put(key, null);
        } else if (value instanceof ObjectRef) {
            String var = ((ObjectRef) value).getVar();
            // If an object does not exist in the map, but is referenced, it's simply an
            // unconstrained object, so we don't much care about its fields
            // Thus, create an empty ObjectInstance (or array)
            // So, create an empty ObjectInstance or array
            Object refValue;
            if (!args.containsKey(var)) {
            	if (type == Integer.class) {
            		Integer v =  2 ;
            		addBoxedPrimitive_ObjectInstance(var,v,Integer.class,IntType.getInstance()) ;
            		converted.put(key, v);
            		return v ;
            	}
            	else if (type == Long.class) {
            		Long v =  2L ;
            		addBoxedPrimitive_ObjectInstance(var,v,Long.class,LongType.getInstance()) ;
            		converted.put(key, v);
            		return v ;
            	}
            	else if (type == Float.class) {
            		Float v =  2f ;
            		addBoxedPrimitive_ObjectInstance(var,v,Float.class,FloatType.getInstance()) ;
            		converted.put(key, v);
            		return v ;
            	}
            	else if (type == Double.class) {
            		Double v =  2d ;
            		addBoxedPrimitive_ObjectInstance(var,v,Double.class,DoubleType.getInstance()) ;
            		converted.put(key, v);
            		return v ;
            	}
            	else if (type == Boolean.class) {
            		Boolean v =  false ;
            		addBoxedPrimitive_ObjectInstance(var,v,Boolean.class,BooleanType.getInstance()) ;
            		converted.put(key, v);
            		return v ;
            	}
            	else if (type.isArray()) {
                    Class<?> componentType = type.getComponentType();
                    Object array = Array.newInstance(componentType, 0);
                    converted.put(key, array);
                    return array;
                } else {;
                    refValue = new ObjectInstance((ClassType) sorts.determineType(type));
                }
            } else {
            	refValue = args.get(var);
            }
            //System.out.println(">>> key:" + key + ", var:" + var + " ty:" + type + ", val:" + refValue) ;
            Object obj = toJava(var, refValue, type);
            converted.put(key, obj);
        } else if (value instanceof ObjectInstance instance) {
        	// Convert ObjectInstance to Object
        	
        	// special case.
            // Classes like Integer and Long appears to have a field called "value" that
            // holds the primitive number it represents, but it is not a real field in the
            // sense that we can't set its value with field.set(v).
            // Handle this differently:
        	String instTyName = instance.type.getFullyQualifiedName() ;
        	Class boxedPrimClass = null ;
        	switch(instTyName) {
        		case "java.lang.Integer" : boxedPrimClass = Integer.class ; break ;
        		case "java.lang.Long"    : boxedPrimClass = Long.class    ; break ;
        		case "java.lang.Float"   : boxedPrimClass = Float.class   ; break ;
        		case "java.lang.Double"  : boxedPrimClass = Double.class  ; break ;
        		case "java.lang.Boolean" : boxedPrimClass = Boolean.class ; break ;
        	}
        	if (boxedPrimClass != null) {
        		Object fieldValue = instance.getFields().get("value").getValue() ;
        		Object convertedValue = toJava(key + "_value", fieldValue, boxedPrimClass);
                converted.put(key, convertedValue);
        		return converted.get(key);
        	}
        	
            // Create a dummy instance that will be filled with the correct values
        	ExecutionResult result = ObjectInstantiation.createInstance(type);
            if (result.isException()) {
                logger.error("Failed to create instance of class: {}", type.getName());
                return null;
            }
            
            Object obj = result.retval();
        
           	// non-special cases
        	
            // WP: to fix non-termination when trying to covert circular object structure,
            // we pre-add the obj to the coverted-list, before filling it its fields: 
            converted.put(key, obj);
               
            // now we fill in the fields of obj:
            for (Map.Entry<String, ObjectField> entry : instance.getFields().entrySet()) {
                try {
                    Field field = ObjectUtils.findField(type, entry.getKey());
                    Object fieldValue = entry.getValue().getValue();
                    Object convertedValue = toJava(key + "_" + entry.getKey(), fieldValue, field.getType());
                    field.set(obj, convertedValue);
                } catch (Exception e) {
                	logger.error("Failed to set field: {} in class: {}", entry.getKey(), type.getName());
                }
            }
            // WP change this logic, as this cause non-termination when trying to convert a circular
            // object structure (e.g. a linked list that is circular).
            // We now put the obj in the coverted-set before we fill in its field.
            // See above:
            // converted.put(key, obj);
        } else if (value.getClass().isArray()) {
            converted.put(key, castArray(value, type));
        } else {
            // Cast to expected type to make sure it is correct
            converted.put(key, type.isPrimitive() ? wrap(type).cast(value)
                    : type.cast(value));
        }

        return converted.get(key);
    }
    
    /**
     * Add an entry to the arg-map, for a var called key, mapping it to a value v of type
     * boxed-primitive e.g. Integer or Long. This creates an ObjectInstance o, and mas
     * key to o. This o has a field called "value", that holds v as a primitive e.g. int
     * (though it will be represented as an Integer v ... i know, a hassle. But the point
     * is to have key to map to an ObjectInstance wrapping v, and a mapping directly to
     * v itself).
     */
    private void addBoxedPrimitive_ObjectInstance(String key, Object v, Class boxedJavaTy, PrimitiveType primSootTy) {
    	ObjectInstance o = new ObjectInstance(JavaAnalyzer.getInstance().getClassType(boxedJavaTy.getName())) ;
		o.setField("value", v, primSootTy);
		args.put(key,o) ;
    }
    

    /**
     * Convert an array of Object to an array of the given type.
     * 
     * @param value The array to convert
     * @param type  The type of the array
     * @return A typed array
     */
    private Object castArray(Object value, Class<?> type) {
        if (type.equals(Object.class)) {
            return value;
        }

        int length = Array.getLength(value);
        Object typedArray = Array.newInstance(type.getComponentType(), length);

        for (int j = 0; j < length; j++) {
            Object element = Array.get(value, j);
            // Copy elements
            Array.set(typedArray, j, toJava("temp", element, type.getComponentType()));
        }
        return typedArray;
    }

    /**
     * Wrap a primitive type in its corresponding wrapper class.
     */
    private static Class<?> wrap(Class<?> clazz) {
        if (!clazz.isPrimitive())
            return clazz;
        if (clazz == int.class)
            return Integer.class;
        if (clazz == long.class)
            return Long.class;
        if (clazz == boolean.class)
            return Boolean.class;
        if (clazz == double.class)
            return Double.class;
        if (clazz == float.class)
            return Float.class;
        if (clazz == char.class)
            return Character.class;
        if (clazz == byte.class)
            return Byte.class;
        if (clazz == short.class)
            return Short.class;
        throw new IllegalArgumentException("Unknown primitive type: " + clazz);
    }

    /**
     * Represents a reference to another variable.
     */
    public static class ObjectRef {
        private final String var;

        public ObjectRef(String var) {
            this.var = var;
        }

        public String getVar() {
            return var;
        }

        @Override
        public String toString() {
            return var;
        }
    }

    /**
     * Represents a field of an object instance.
     */
    public static class ObjectField {
        private final Object value;
        private final Type type;

        public ObjectField(Object value, Type type) {
            this.value = value;
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public Type getType() {
            return type;
        }
    }

    /**
     * Represents an object and its fields.
     */
    public static class ObjectInstance {
        private final ClassType type;
        private final Map<String, ObjectField> fields;

        public ObjectInstance(ClassType type) {
            this.type = type;
            this.fields = new HashMap<>();
        }

        public ClassType getType() {
            return type;
        }

        public Map<String, ObjectField> getFields() {
            return fields;
        }

        public void setField(String name, Object value, Type type) {
            fields.put(name, new ObjectField(value, type));
        }

        public boolean hasField(String name) {
            return fields.containsKey(name);
        }
        
        @Override
        public String toString() {
        	String s = "" + type + "-> " ;
        	for (var x : fields.entrySet()) {
        		var v = x.getValue().getValue() ;
        		s += x.getKey() + ":" + v + "/" + v.getClass().getSimpleName() ;
        		s += ", " ;
        	}
        	return s ;
        }
    }
}
