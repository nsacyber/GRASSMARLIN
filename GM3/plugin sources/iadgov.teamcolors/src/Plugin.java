package iadgov.teamcolors;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ColorFactoryMenuItem;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A completely pointless plugin that adds NFL team colors as an aggregate color theme.
 * The demand for this plugin was far greater than any other proposed plugin (including diff), so here it is, please stop asking for it.
 * This file contains no typos.  Everything is exactly as required by the specification.
 */
@IPlugin.Uses("grassmarlin")
public class Plugin implements grassmarlin.plugins.IPlugin, grassmarlin.plugins.IPlugin.HasVersionInfo {
    private final static Color[] borders = {
            /* Bills        */ Color.rgb(198, 12, 48),
            /* Dolphins     */ Color.rgb(245, 130, 32),
            /* Patriots     */ Color.rgb(198, 12, 48),
            /* Jets         */ Color.rgb(32, 55, 49),
            /* Ravens       */ Color.rgb(158, 124, 12),
            /* Bengals      */ Color.rgb(251, 79, 20),
            /* Browns       */ Color.rgb(49, 29, 0),
            /* Steelers     */ Color.rgb(255, 182, 18),
            /* Texans       */ Color.rgb(3, 32, 47),
            /* Colts        */ Color.rgb(0, 44, 95),
            /* Jaguars      */ Color.rgb(159, 121, 44),
            /* Titans       */ Color.rgb(75, 146, 219),
            /* Broncos      */ Color.rgb(251, 79, 20),
            /* Chiefs       */ Color.rgb(255, 182, 18),
            /* Chargers     */ Color.rgb(255, 182, 18),
            /* Raiders      */ Color.rgb(165, 172, 175),
            // Cowboys      */ Color.rgb(176, 183, 188),
            /* Giants       */ Color.rgb(167, 25, 48),
            /* Eagles       */ Color.rgb(165, 172, 175),
            /* Redskins     */ Color.rgb(255, 182, 18),
            /* Bears        */ Color.rgb(200, 56, 3),
            /* Lions        */ Color.rgb(176, 183, 188),
            /* Packers      */ Color.rgb(255, 182, 18),
            /* Vikings      */ Color.rgb(255, 182, 18),
            /* Falcons      */ Color.rgb(0, 0, 0),
            /* Panthers     */ Color.rgb(0, 133, 202),
            /* Saints       */ Color.rgb(159, 137, 88),
            /* Buccaneers   */ Color.rgb(52, 48, 43),
            /* Cardinals    */ Color.rgb(0, 0, 0),
            /* Rams         */ Color.rgb(179, 153, 93),
            /* 49ers        */ Color.rgb(179, 153, 93),
            /* Seachickens  */ Color.rgb(105, 190, 40)
    };
    private final static Color[] fills = {
            /* Bills        */ Color.rgb(0, 51, 141),
            /* Dolphins     */ Color.rgb(0, 142, 151),
            /* Patriots     */ Color.rgb(0, 34, 68),
            /* Jets         */ Color.rgb(255, 255, 255),
            /* Ravens       */ Color.rgb(36, 23, 115),
            /* Bengals      */ Color.rgb(0, 0, 0),
            /* Browns       */ Color.rgb(255, 60, 0),
            /* Steelers     */ Color.rgb(0, 0, 0),
            /* Texans       */ Color.rgb(167, 25, 48),
            /* Colts        */ Color.rgb(0, 44, 95),
            /* Jaguars      */ Color.rgb(0, 103, 120),
            /* Titans       */ Color.rgb(0, 34, 68),
            /* Broncos      */ Color.rgb(0, 34, 68),
            /* Chiefs       */ Color.rgb(227, 24, 55),
            /* Chargers     */ Color.rgb(0, 115, 207),
            /* Raiders      */ Color.rgb(0, 0, 0),
            // Cowboys      */ Color.rgb(0, 51, 141),
            /* Giants       */ Color.rgb(11, 34, 101),
            /* Eagles       */ Color.rgb(0, 76, 84),
            /* Redskins     */ Color.rgb(102, 0, 0),
            /* Bears        */ Color.rgb(11, 22, 42),
            /* Lions        */ Color.rgb(0, 90, 139),
            /* Packers      */ Color.rgb(32, 55, 49),
            /* Vikings      */ Color.rgb(79, 38, 131),
            /* Falcons      */ Color.rgb(167, 25, 48),
            /* Panthers     */ Color.rgb(0, 0, 0),
            /* Saints       */ Color.rgb(0, 0, 0),
            /* Buccaneers   */ Color.rgb(213, 10, 10),
            /* Cardinals    */ Color.rgb(151, 35, 63),
            /* Rams         */ Color.rgb(0, 34, 68),
            /* 49ers        */ Color.rgb(170, 0, 0),
            /* Seachickens  */ Color.rgb(165, 172, 175)
    };

    private int idxNext;
    private final Map<Object, Integer> values;

    public Plugin(final RuntimeConfiguration config) {
        this.idxNext = (int)(Math.random() * (double)fills.length); //Random starting point, because if it isn't random we're playing favorites, and playing favorites would probably get me killed.
        this.values = new HashMap<>();

        final ILogicalViewApi gm = config.pluginFor("grassmarlin", grassmarlin.Plugin.class);
        gm.addGroupColorFactory(() -> new GenerateColors());
    }

    private class GenerateColors implements ColorFactoryMenuItem.IColorFactory {
        private final CheckMenuItem mi = new CheckMenuItem("Team Colors");

        private int idForObject(final Object o) {
            synchronized(Plugin.this.values) {
                final Integer existing = Plugin.this.values.get(o);
                if(existing == null) {
                    final int id = Plugin.this.idxNext++;
                    Plugin.this.values.put(o, id);
                    return id;
                } else {
                    return existing;
                }
            }
        }

        @Override
        public CheckMenuItem getMenuItem() {
            return this.mi;
        }

        @Override
        public Color getBorderColor(final Object o) {
            return Plugin.borders[idForObject(o) % Plugin.borders.length];
        }
        @Override
        public Color getBackgroundColor(final Object o) {
            final Color base = Plugin.fills[idForObject(o) % Plugin.fills.length];
            return Color.rgb((int)(base.getRed() * 255.0), (int)(base.getGreen() * 255.0), (int)(base.getBlue() * 255.0));
        }

    }

    @Override
    public String getName() {
        return "Team Colors";
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
