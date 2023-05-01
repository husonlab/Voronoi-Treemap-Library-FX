/*
 * TreeNode.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.husonlab.voronoitreemapservice.example;

import java.util.ArrayList;
import java.util.Map;

/**
 * simple example tree node
 * Daniel Huson, 5.2023
 */
public class TreeNode {
	private final ArrayList<TreeNode> children = new ArrayList<>();
	private TreeNode parent;
	private final String name;

	public TreeNode(String name) {
		this.name = name;
	}

	public ArrayList<TreeNode> getChildren() {
		return children;
	}

	public String getName() {
		return name;
	}

	public double getWeight() {
		if (getChildren().size() == 0)
			return 1.0;
		else
			return 0;
	}

	public boolean isBelow(TreeNode node) {
		var v = this;
		while (v != null) {
			if (v == node)
				return true;
			v = v.parent;
		}
		return false;
	}

	public boolean isBelow(String name) {
		var v = this;
		while (v != null) {
			if (v.getName().equals(name))
				return true;
			v = v.parent;
		}
		return false;
	}

	public static void createLink(Map<String, TreeNode> nameNodeMap, String childName, String parentName) {
		var parent = nameNodeMap.computeIfAbsent(parentName, TreeNode::new);
		var child = nameNodeMap.computeIfAbsent(childName, TreeNode::new);

		child.parent = parent;

		if (!parent.getChildren().contains(child))
			parent.getChildren().add(child);
	}
}
