/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.web.controller.router;

import java.util.LinkedList;

import org.exoplatform.web.controller.regexp.GroupType;
import org.exoplatform.web.controller.regexp.RENode;
import org.exoplatform.web.controller.regexp.REVisitor;

class CaptureGroupTransformation extends REVisitor<RuntimeException> {

    /** . */
    private int depth;

    /** Top level group per disjunction. */
    private LinkedList<RENode.Group> groups;

    CaptureGroupTransformation() {
        this.depth = 0;
        this.groups = new LinkedList<RENode.Group>();
    }

    @Override
    protected void visit(RENode.Disjunction disjunction) throws RuntimeException {
        if (disjunction.hasAlternative()) {
            RENode.Alternative alternative = disjunction.getAlternative();
            if (alternative != null) {
                alternative.accept(this);

                //
                if (depth == 0) {
                    if (groups.size() == 1 && groups.get(0).getQuantifier() == null) {
                        // Do nothing
                    } else {
                        // We make all the top level groups non capturing
                        for (RENode.Group group : groups) {
                            group.setType(GroupType.NON_CAPTURING_GROUP);
                        }

                        // We add a capturing group for the disjunction
                        RENode.Disjunction disjunction1 = new RENode.Disjunction((RENode.Alternative) null);
                        RENode.Group group = new RENode.Group(disjunction1, GroupType.CAPTURING_GROUP);
                        RENode.Alternative alternative1 = new RENode.Alternative(group);
                        alternative.replaceBy(alternative1);
                        disjunction1.setAlternative(alternative);
                    }

                    //
                    groups.clear();
                }
            } else {
                if (depth == 0) {
                    disjunction.setAlternative(new RENode.Alternative(new RENode.Group(new RENode.Disjunction(),
                            GroupType.CAPTURING_GROUP)));
                }
            }
        }

        //
        if (disjunction.hasNext()) {
            RENode.Disjunction next = disjunction.getNext();
            if (next != null) {
                next.accept(this);
            } else {
                if (depth == 0) {
                    disjunction.setNext(new RENode.Disjunction(new RENode.Alternative(new RENode.Group(
                            new RENode.Disjunction(), GroupType.CAPTURING_GROUP))));
                }
            }
        }
    }

    @Override
    protected void visit(RENode.Group expr) throws RuntimeException {
        if (depth == 0) {
            // We collect all capturing top level capturing groups
            if (expr.getType() == GroupType.CAPTURING_GROUP) {
                groups.add(expr);
            }
        } else {
            // We make nested capturing group as non capturing
            if (expr.getType() == GroupType.CAPTURING_GROUP) {
                expr.setType(GroupType.NON_CAPTURING_GROUP);
            }
        }

        //
        depth++;
        super.visit(expr);
        depth--;
    }
}
