/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import ICSDefines.Category;
import ICSDefines.Role;
import java.awt.Image;
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import ui.icon.Icons;

/**
 *
 */
public interface Entities <T extends Entities> {
 
    /** Map key */
    static final String NAME_FIELD = "Name";
    /** Map key */
    static final String PREFERRED_NAME = "Preferred Name";
    /** Map key */
    static final String CATEGORY_FIELD = "Category";
    /** Map key */
    static final String ROLE_FIELD = "Role";
    /** Map key */
    static final String CONFIDENCE_FIELD = "Confidence";
    /** Map key */
    static final String COUNTRY_FIELD = "Country Name";
    /** Map key */
    static final String FLAG_FIELD = "Flag Image";
    /** Map key */
    static final String ICON_FIELD = "Selected Icon";
    /** Map key */
    static final String COMMON_FIELD = "Common Properties";
    /** Map key */
    static final String SOURCE_FILE_FIELD = "File";
    /** Map key */
    static final String VENDOR_FIELD = "Vendor";
    /** Map key */
    static final String HARDWARE_ADDRESS_FIELD = "Hardware Address";
    /** Map key */
    static final String DESCRIPTION_KEY = "Description";
    /** Map key */
    static final String HARDWARE_MODEL = "Hardware Model";
    
    Map<String, Object> getAttributes();
    
    Map<String, Object> getDefaults();
    
    static Map<String,Object> newDefaults() {
        Map<String,Object> map = new HashMap<>();
        HashMap<String,Set<String>> common = new HashMap<>();
        map.put(NAME_FIELD, "");
        map.put(PREFERRED_NAME, "");
        map.put(CATEGORY_FIELD, Category.UNKNOWN);
        map.put(ROLE_FIELD, Role.UNKNOWN);
        map.put(CONFIDENCE_FIELD, 1);
        map.put(COUNTRY_FIELD, "");
        map.put(FLAG_FIELD, null);
        map.put(VENDOR_FIELD, "");
        map.put(ICON_FIELD, Icons.Original_unknown);
        map.put(COMMON_FIELD, common);
        map.put(SOURCE_FILE_FIELD, null);
        return map;
    }
    
    default Object getDefault(String field) {
        return getDefaults().get(field);
    }
    
    default public Object getOrDefault(String field, Object defaultValue) {
        Object obj = getAttributes().get(field);
        if( obj == null ) {
            obj = defaultValue;
        }
        return obj;
    }
    
    default public void set(String field, Object value) {
        getAttributes().put(field, value);
    }
    
    default public boolean canGet(String field) {
        Object obj = getAttributes().get(field);
        return obj != null;
    }
    
    default public Object get(String field) {
        Object obj = getAttributes().get(field);
        if( obj == null ) {
            obj = getDefault(field);
        }
        return obj;
    }
    
    default Object get(String field, Class clazz) {
        Object obj = get(field);
        if( obj != null && !clazz.isAssignableFrom(obj.getClass()) ) {
            throw new java.lang.ClassCastException(String.format("\"%s\" IS TYPE %s; EXPECTED %s.", field, obj.getClass(), clazz));
        }
        return obj;
    }
    
    default String getString(String field) {
        return (String) get(field, String.class);
    }
    
    default Integer getInt(String field) {
        return (Integer) get(field, Integer.class);
    }
    
    default Image getImage(String field) {
        return (Image) get(field, Image.class);
    }
    
    default Collection getCollection(String field) {
        return (Collection) get(field, Collection.class);
    }
    
    default Icons getIcon(String field) {
        return (Icons) get(field, Icons.class);
    }

    default T setIcon(Icons icon) {
        set(ICON_FIELD, icon);
        return (T)this;
    }
    
    default Image getIconImage(String field) {
        return ((Icons) get(field, Image.class)).get32();
    }
    
    default Map<String,Set<String>> getDataMap(String field) {
        return (Map<String,Set<String>>) get(field, Map.class);
    }
    
    default File getFile(String field) {
        return (File) get(field, File.class);
    }
    
    default String getName() {
        return getString(NAME_FIELD);
    }
    
    default T setHardwareModel(String model) {
        set(HARDWARE_MODEL, model);
        return (T)this;
    }
    
    default T setName(String name) {
        set(NAME_FIELD, name);
        return (T)this;
    }
    
    default String getPrefferedName() {
        return getString(PREFERRED_NAME);
    }
    
    default T setPrefferedName(String name) {
        set(PREFERRED_NAME, name);
        return (T)this;
    }
    
    default T setDescription(String description) {
        set(DESCRIPTION_KEY, description);
        return (T) this;
    }
    
    default String getVendor() {
        return getString(VENDOR_FIELD);
    }
    
    default T setVendor(String vendor) {
        set(VENDOR_FIELD, vendor);
        return (T)this;
    }
    
    default boolean hasSpecifiedName() {
        return !getName().equals(getPrefferedName());
    }
    
    default Integer getConfidence() {
        return getInt(CONFIDENCE_FIELD);
    }
    
    default Category getCategory() {
        return (Category) get(CATEGORY_FIELD);
    }

    default T setCategory(Category category) {
        set(CATEGORY_FIELD, category);
        return (T)this;
    }
    
    default Role getRole() {
        return (Role) get(ROLE_FIELD);
    }
    
    default T setRole(Role role) {
        set(ROLE_FIELD, role);
        return (T)this;
    }
    
    default String getCountry() {
        return getString(COUNTRY_FIELD);
    }
    
    default boolean hasFlag() {
        Object obj = get(FLAG_FIELD);
        return obj != null;
    }
    
    default Image getFlag() {
        return getImage(FLAG_FIELD);
    }
    
    default Image getImage16() {
        Image image;
        if( hasFlag() ) {
            image = getFlag();
        } else {
            image = getIcon(ICON_FIELD).get();
        }
        return image;
    }
    
    default Map<String,Set<String>> getCommon() {
        return getDataMap(COMMON_FIELD);
    }
    
    default File getSourceFile() {
        return getFile(SOURCE_FILE_FIELD);
    }
    
    default T setSourceFile(File file) {
        set(SOURCE_FILE_FIELD, file);
        return (T)this;
    }
    
    default boolean hasSourceFile() {
        return canGet(SOURCE_FILE_FIELD);
    }
    
    default void copyAll(Entities entity) {
        getAttributes().putAll(entity.getAttributes());
    }
    
    public abstract class BasicEntity<T extends Entities> implements Entities<T>, Serializable  {

        private static final Map<String,Object> DEFAULTS = Collections.unmodifiableMap( Entities.newDefaults() );
        private final Map<String,Object> attributes;

        public BasicEntity(Entities entity) {
            if( entity != null ) {
                this.attributes = entity.getAttributes();
            } else {
                this.attributes = new HashMap<>();
            }
        }
        
        public BasicEntity() {
            this.attributes = new HashMap<>();
        }
        
        /**
         * Iterates through any and all key,Value-list pairs in the attributes of this entity.
         * @param callback Callback accepts every key-value pair where the value is an instance of Collection.
         */
        public void eachDataSet(BiConsumer<String, Collection<String>> callback) {
            getAttributes().entrySet().stream()
                    .filter(e -> e.getValue() instanceof Collection)
                    .forEach( e -> callback.accept(e.getKey(), (Collection)e.getValue()));
        }
        
        @Override
        public Map<String, Object> getAttributes() {
            return this.attributes;
        }

        @Override
        public Map<String, Object> getDefaults() {
            return DEFAULTS;
        }

    }
    
}
