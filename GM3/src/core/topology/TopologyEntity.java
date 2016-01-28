/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

/**
 *
 * @param <T> Type of the aggregate owner
 */
public abstract class TopologyEntity<T> extends Entities.BasicEntity implements Comparable {

    public static final String GROUP_FIELD = "Group";
    public static final String ID_FIELD = "Id";
    
    public static enum Type {

        PORT, HOST, SWITCH, CLOUD, DEFAULT, HOST_PORT, CLOUD_HOST, PORT_CLOUD, PORT_PORT, INTERFACE, NETWORK;

        public static Type determineEdge(TopologyEntity e0, TopologyEntity e1) {
            Type t0 = e0.type;
            Type t1 = e1.type;
            Type ret = Type.DEFAULT;
            if (eitherEquals(t0, t1, HOST, PORT)) {
                ret = Type.HOST_PORT;
            } else if (eitherEquals(t0, t1, PORT, PORT)) {
                ret = Type.PORT_PORT;
            } else if (eitherEquals(t0, t1, PORT, CLOUD)) {
                ret = Type.PORT_CLOUD;
            } else if (eitherEquals(t0, t1, CLOUD, HOST)) {
                ret = Type.CLOUD_HOST;
            }
            return ret;
        }

        private static boolean eitherEquals(Type t0, Type t1, Type other0, Type other1) {
            return (t0.equals(other0) && t1.equals(other1))
                    || (t0.equals(other1) && t1.equals(other0));
        }

    }

    /**
     * The index of the visual row assigned by the visualization
     */
    int visualRow;
    /**
     * The index of the visual Aggregate or parent VisualTuple row.
     */
    int visualAgg;
    /**
     * The type of topology entity this object represents.
     */
    public final Type type;
    
    public TopologyEntity(Type topologyType, Entities entity) {
        super(entity);
        this.visualRow = this.visualAgg = -1;
        this.type = topologyType;
    }
    
    public TopologyEntity(Type topologyType) {
        super();
        this.visualRow = this.visualAgg = -1;
        this.type = topologyType;
    }

    public TopologyEntity() {
        this(Type.DEFAULT);
    }

    public String typeName() {
        return type.name();
    }
    
    public abstract Mac getMac();
    
    public boolean hasPort() {
        return false;
    }
    
    public Port getPort() {
        return null;
    }
    
    public abstract String getDisplayText();

    public abstract void resetDisplayText();

    /**
     * Retrieves the first set of data for which this entity is creates from.
     *
     * @return
     */
    public abstract T getDataSet();

    public int getVisualAgg() {
        return visualAgg;
    }

    public void setVisualAgg(int visualAgg) {
        this.visualAgg = visualAgg;
        this.set(GROUP_FIELD, visualRow);
    }

    public int getVisualRow() {
        return visualRow;
    }

    public void setVisualRow(int visualRow) {
        this.visualRow = visualRow;
        this.set(ID_FIELD, visualRow);
    }

    public final void visualReset() {
        this.visualAgg = -1;
        this.visualAgg = -1;
    }
}
