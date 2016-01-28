/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.types;

import ICSDefines.Category;
import ICSDefines.Direction;
import ICSDefines.Role;
import TemplateEngine.Data.PseudoDetails;
import java.util.HashSet;
import java.util.Set;

/**
 * @author BESTDOG Basic object containing parameters seen on each piece of
 * network data.  <pre>
 * {@link #confidence} is assigned to data on a scale of 0-5. It may only be
 * increased by predication of fingerprints.
 * {@link #role} describes the purpose of this data where its seen, its another
 * level of granularity finer than {@link #category}.
 * {@link #category} a generic category of the physical device this data is
 * collected from.
 * {@link #direction} declares this data is being seen by a sensor as either a
 * source or destination of traffic.
 * {@link #names} arbitrary set of names, likely used to record the fingerprints
 * which populate this data.
 * </pre>
 */
public class DataDetails extends PseudoDetails {
    
    protected Integer confidence;
    protected Role role;
    protected Direction direction;
    protected Category category;
    protected final HashSet<String> names;

    public DataDetails() {
        confidence = 0;
        role = Role.UNKNOWN;
        category = Category.UNKNOWN;
        direction = Direction.SOURCE;
        names = new HashSet<>();
    }

    public HashSet<String> getFingerprintNames() {
        return names;
    }

    /**
     * @return The direction of this data, collected and seen as a source or
     * destination.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * @return The confidence / accuracy of this data on a scale of 1 to five.
     */
    public Integer getConfidence() {
        return confidence;
    }

    /**
     * @return The category of this data.
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return The role of this data.
     */
    public Role getRole() {
        return role;
    }
    
    
    public static final String KEY_NAME = "Name";
    /**
     * Sets the KEY_NAME for this Map.
     * @param name name to set.
     */
    public void setName(String name) {
        put(KEY_NAME, name);
    }
    
    /**
     * @return Value for the KEY_NAME key if set, else null.
     */
    public String getName() {
        return get(KEY_NAME);
    }
    
    /**
     * Confidences can only be increased from 0, never decreased.
     *
     * @param confidence Confidence to assign to this data.
     */
    public void setConfidence(Integer confidence) {
        if (this.confidence < confidence) {
            this.confidence = confidence;
        }
        bindAll();
    }

    /**
     * @param category Category to assign to this data.
     * @param setIfUnknown Will only set category if {@link #getCategory() } equals UNKNOWN if true, else false and check is ignored.
     */
    public void setCategory(Category category, boolean setIfUnknown) {
        if( setIfUnknown && !Category.UNKNOWN.equals(category) ) {
            this.setCategory(category);
        }
    }
    
    /**
     * @param category Category to assign to this data.
     */
    public void setCategory(Category category) {
        this.category = category;
        bindAll();
    }

    /**
     * @param direction Direction to assign to this data.
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
        bindAll();
    }

    /**
     * @param role Role to assign to this data.
     */
    public void setRole(Role role) {
        this.role = role;
        bindAll();
    }

    /**
     * @param name Name to add to a set of names, usually used to store
     * fingerprint names.
     */
    public void putName(String name) {
        names.add(name);
    }

    public Set<String> getNames() {
        return names;
    }
    
    private final static String COUNTRY = "Country";
    public String getCountry() {
        return getOrDefault(COUNTRY, "");
    }
    
    public void setCountry(String country) {
        if( country != null ) {
            put(COUNTRY, country);
        }
    }
    
    private final static String KEY_HARDWARE_VENDOR = "Vendor Name";
    public String getHardwareVendor() {
        return getOrDefault(KEY_HARDWARE_VENDOR, "");
    }
    
    public void setHardwareVendor(String vendor) {
        if( vendor != null ) {
            put(KEY_HARDWARE_VENDOR, vendor);
        }
    }
    
    public static final String KEY_HARDWARE_TYPE = "Vendor Type";
    public String getHardwareType() {
        return getOrDefault(KEY_HARDWARE_TYPE, "");
    }
    
    public void setHardwareType(String vendor) {
        if( vendor != null ) {
            put(KEY_HARDWARE_TYPE, vendor);
        }
    }
    
    public void copy(DataDetails other) {
        super.putAll(other);
        this.names.addAll(other.names);
        this.setCategory(other.category, true);
        this.setConfidence(other.confidence);
        this.setDirection(other.direction);
        this.setRole(other.role);
    }
    
    @Override
    public void bindAll() {
        super.bindAll();
        put("Category", getCategory().getPrettyPrint());
        put("Role", getRole().getPrettyPrint());
        put("Confidence", getConfidence().toString());
        put("Direction", getDirection().name());
    }
    
    @Override
    public void clear() {
        super.clear();
        confidence = 0;
        role = Role.OTHER;
        direction = Direction.SOURCE;
        category = Category.OTHER;
        names.clear();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        bindAll();
        return super.entrySet();
    }
    
}
