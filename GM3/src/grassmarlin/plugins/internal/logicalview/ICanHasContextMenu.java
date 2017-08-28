package grassmarlin.plugins.internal.logicalview;

import javafx.scene.control.MenuItem;

import java.util.List;

/**
 * Contrary to popular belief, cats are quite adept at spelling and seldom mistake a "z" for an "s".  This code was also
 * written by humans, not cats, and yet the temptation to use "Haz" was remarkably high.  The logical conclusion is that
 * humans aspire to be as dumb as we mistakenly believe cats to be.  My cat agrees with this assessment.
 */
public interface ICanHasContextMenu {
    List<MenuItem> getContextMenuItems();
}
