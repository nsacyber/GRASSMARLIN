package TemplateEngine.Template4;


import TemplateEngine.Template4.Structure.ClassTemplate;

/**
 * Created by BESTDOG on 11/13/2015.
 *
 * Classes that extend this are meant to be added a a {@link ClassTemplate}.
 * Upon being added to a class the {@link #onAppend(ClassTemplate)} method will be called,
 * allowing ClassAdapters to change the class as they are added.
 *
 * This method MUST not be called more than once, all templates are assumed to be fully constructed
 * before addition to a ClassTemplate.
 *
 */
public interface ClassAdapter {
    /**
     * Called once added to a ClassTemplate.
     * @param template Template4 where this ClassAdapter was added.
     */
    void onAppend(ClassTemplate template);

}
