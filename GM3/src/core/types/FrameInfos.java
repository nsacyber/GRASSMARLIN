/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.types;

/**
 * <pre>
 * 
 * </pre>
 */
public class FrameInfos {
    public final Long packet;
    public final Long date;
    public final Integer size;
    
    public FrameInfos(Long packet, Integer size, Long date) {
        this.packet = packet;
        this.date = date;
        this.size = size;
    }
}
