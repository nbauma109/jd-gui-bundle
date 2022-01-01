/*
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jdv1.gui.service.actions;

import org.jd.core.v1.cfg.ControlFlowGraphPlantUMLWriter;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphGotoReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphLoopReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;
import org.jd.core.v1.util.StringConstants;
import org.jd.gui.api.API;
import org.jd.gui.api.model.Container;
import org.jd.gui.spi.ContextualActionsFactory;
import org.jd.gui.util.ImageUtil;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.ImageIcon;

public class ShowControlFlowGraphContextualActionsFactory implements ContextualActionsFactory {

    public static final int MODE_RAW = 0;
    public static final int MODE_GOTO_ONLY = 1;
    public static final int MODE_GOTO_AND_LOOP = 2;
    
    @Override
    public Collection<Action> make(API api, Container.Entry entry, String fragment) {
        Collection<Action> actions = new ArrayList<>();
        if (entry.getPath().endsWith(StringConstants.CLASS_FILE_SUFFIX)) {
            actions.add(new ShowControlFlowGraphAction(entry, fragment, null, MODE_RAW));
            actions.add(new ShowControlFlowGraphAction(entry, fragment, null, MODE_GOTO_ONLY));
            actions.add(new ShowControlFlowGraphAction(entry, fragment, null, MODE_GOTO_AND_LOOP));
            for (ControlFlowGraphReducer controlFlowGraphReducer : ControlFlowGraphReducer.getPreferredReducers()) {
                actions.add(new ShowControlFlowGraphAction(entry, fragment, controlFlowGraphReducer, MODE_GOTO_AND_LOOP));
            }
        }
        return actions;
    }

    public static class ShowControlFlowGraphAction extends AbstractMethodAction {

        private static final long serialVersionUID = 1L;

        protected static final ImageIcon ICON = new ImageIcon(ImageUtil.getImage("/net/sourceforge/plantuml/version/favicon.png"));

        private final transient ControlFlowGraphReducer controlFlowGraphReducer;
        
        private int mode;

        public ShowControlFlowGraphAction(Container.Entry entry, String fragment, ControlFlowGraphReducer controlFlowGraphReducer, int mode) {
            super(entry, fragment);
            this.controlFlowGraphReducer = controlFlowGraphReducer;
            this.mode = mode;
            if (controlFlowGraphReducer == null) {
                putValue(GROUP_NAME, "Edit > ShowInitialControlFlowGraph"); // used for sorting and grouping menus
                putValue(NAME, "Show Initial Control Flow Graph (" + getModeAsString() + ")");
            } else {
                putValue(GROUP_NAME, "Edit > ShowReducedControlFlowGraph");
                putValue(NAME, controlFlowGraphReducer.getLabel());
            }
            putValue(SMALL_ICON, ICON);
        }

        private String getModeAsString() {
            return switch (mode) {
                case MODE_RAW -> "Raw";
                case MODE_GOTO_ONLY -> "Goto Only";
                case MODE_GOTO_AND_LOOP -> "Goto And Loop";
                default -> throw new IllegalArgumentException("Unexpected value: " + mode);
            };
        }

        protected void methodAction(Method method) {
            if (controlFlowGraphReducer == null) {
                ControlFlowGraph controlFlowGraph = new ControlFlowGraphMaker().make(method);
                switch (mode) {
                    case MODE_GOTO_ONLY:
                        ControlFlowGraphGotoReducer.reduce(controlFlowGraph);
                        break;
                    case MODE_GOTO_AND_LOOP:
                        ControlFlowGraphGotoReducer.reduce(controlFlowGraph);
                        ControlFlowGraphLoopReducer.reduce(controlFlowGraph);
                        break;
                    default:
                        break;
                }
                ControlFlowGraphPlantUMLWriter.showGraph(controlFlowGraph);
            } else {
                controlFlowGraphReducer.reduce(method);
                ControlFlowGraphPlantUMLWriter.showGraph(controlFlowGraphReducer.getControlFlowGraph());
            }
        }
    }
}
