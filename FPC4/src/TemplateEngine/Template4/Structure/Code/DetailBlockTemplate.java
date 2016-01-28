package TemplateEngine.Template4.Structure.Code;

import ICSDefines.Category;
import ICSDefines.Direction;
import ICSDefines.Role;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.DetailGroup;
import TemplateEngine.Fingerprint3.Return;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.RegularTemplate;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by BESTDOG on 11/13/2015.
 */
public class DetailBlockTemplate extends RegularTemplate {

    private enum $ implements TemplateAccessor {
        FingerprintName,
        Confidence,
        Direction,
        Category,
        Role,
        Details;
    }

    String fingerprintName;
    Integer confidence;
    Object[][] details;
    Direction direction;
    Category category;
    Role role;

    protected DetailBlockTemplate(String templateName) {
        super(templateName);
    }

    public DetailBlockTemplate() {
        this("DetailBlock");
    }

    private DetailBlockTemplate(DetailBlockTemplate original) {
        this();
        this.setTemplate(original.getTemplate());
    }

    public DetailBlockTemplate newTemplate() {
        return new DetailBlockTemplate(this);
    }

    public DetailBlockTemplate setFingerprintName(String fingerprintName) {
        this.fingerprintName = fingerprintName;
        return this;
    }

    public DetailBlockTemplate setReturn(Return return_) {
        this.confidence = return_.getConfidence();
        this.direction = getDirection(return_);
        this.category = getCategory(return_);
        this.role = getRole(return_);
        this.details = getDetails(return_);
        return this;
    }

    private Object[][] getDetails(Return r) {
        Object[][] obj = null;
        DetailGroup dg = r.getDetails();
        if( dg != null ) {
            List<DetailGroup.Detail> details = dg.getDetail();
            if( details != null && !details.isEmpty() ) {
                obj = new Object[details.size()][];
                int i = 0;
                for (DetailGroup.Detail detail : details) {
                    String name = detail.getName();
                    String val = detail.getValue();
                    obj[i]  = new Object[] { name, val };
                }
            }
        }
        return obj;
    }

    private Role getRole(Return r) {
        Role val = null;
        try {
            if(  r.getDetails() != null && r.getDetails().getRole() != null ) {
                val = Role.valueOf(r.getDetails().getRole());
            }
        } catch( Exception ex ) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error in xml.", ex);
        }
        return val;
    }

    private Category getCategory(Return return_) {
        Category val = null;
        try {
            if( return_.getDetails() != null ) {
                String category = return_.getDetails().getCategory();
                if( category != null ) {
                    val = Category.valueOf(category);
                }
            }
        } catch( Exception ex ) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error in xml.", ex);
        }
        return val;
    }

    private Direction getDirection(Return return_) {
        Direction val = null;
        try {
            val = Direction.valueOf(return_.getDirection());
        } catch( Exception ex ) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error in xml.", ex);
        }
        return val;
    }

    public String getDirection() {
        String string = null;
        if( this.direction != null ) {
            string = String.format("%s.%s", Direction.class.getSimpleName(), this.direction);
        }
        return string;
    }

    public String getCategory() {
        String string = null;
        if( this.category != null ) {
            string = String.format("%s.%s", Category.class.getSimpleName(), this.category.name());
        }
        return string;
    }

    public String getRole() {
        String string = null;
        if( this.role != null ) {
            string = String.format("%s.%s", Role.class.getSimpleName(), this.role);
        }
        return string;
    }

    public Object[][] getDetails() {
        return this.details;
    }

    @Override
    public void render(ST st) {
        $.FingerprintName.add(st, this.fingerprintName);
        $.Confidence.add(st, this.confidence);
        $.Direction.add(st, getDirection());
        $.Category.add(st, getCategory());
        $.Role.add(st, getRole());
        $.Details.add(st, getDetails());
    }

}
