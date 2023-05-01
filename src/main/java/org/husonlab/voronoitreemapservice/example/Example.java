/*
 * Example.java Copyright (C) 2023 Daniel H. Huson
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

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import org.husonlab.voronoitreemapservice.PolygonUtilities;
import org.husonlab.voronoitreemapservice.Settings;
import org.husonlab.voronoitreemapservice.VoronoiTreeMapService;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class Example extends Application {
	private double zoom = 1.0;

	@Override
	public void start(Stage stage) {
		var circle = new Circle(350);
		circle.setFill(Color.TRANSPARENT);
		circle.setStroke(Color.LIGHTGRAY);
		circle.setStrokeWidth(4);

		var vbox = new VBox();
		vbox.setStyle("-fx-spacing: 10;");
		var borderPane = new BorderPane();
		borderPane.setStyle("-fx-padding: 10 10 10 10;");
		borderPane.setRight(vbox);

		var cancel = new ToggleButton("Cancel");
		//cancel.setSelected(true);
		var scene = new Scene(borderPane, 900, 900);

		stage.setScene(scene);
		stage.setTitle("Voronoi Tree Map FX Example");
		stage.show();

		borderPane.prefHeightProperty().bind(scene.heightProperty());

		var outerGroup = new Group();
		var scrollPane = new ScrollPane(new StackPane(new Group(outerGroup)));
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		scrollPane.setPannable(true);
		borderPane.setCenter(scrollPane);

		var rootNode = setupTree();
		var rootPolygon = PolygonUtilities.simpleNGon(350, 48);

		var backgroundPolygon = PolygonUtilities.polygon(rootPolygon);
		backgroundPolygon.setFill(Color.TRANSPARENT);
		backgroundPolygon.setStroke(Color.LIGHTGRAY);
		outerGroup.getChildren().add(backgroundPolygon);

		var levelsGroup = new Group();
		var labelsGroup = new Group();
		outerGroup.getChildren().addAll(levelsGroup, labelsGroup);

		var voronoiTreeMapService = new VoronoiTreeMapService<TreeNode>(new Settings(16, 666, 0.98));

		BiConsumer<TreeNode, PolygonSimple> resultConsumer = (v, p) -> {
			var level = p.getLevel();
			while (level >= levelsGroup.getChildren().size()) {
				levelsGroup.getChildren().add(new Group());
				labelsGroup.getChildren().add(new Group());
			}
			var polygon = PolygonUtilities.polygon(p);
			if (v.isBelow("Archaea"))
				polygon.setFill(Color.web("#DF5C24").deriveColor(1, 1, 1, 0.1));
			else if (v.isBelow("Bacteria"))
				polygon.setFill(Color.web("#265DAB").deriveColor(1, 1, 1, 0.1));
			else
				polygon.setFill(Color.web("#059748").deriveColor(1, 1, 1, 0.1));
			polygon.setStroke(Color.DARKGRAY);

			var label = new Label(v.getName());
			label.setFont(layerToFont(level));
			var center = PolygonUtilities.computeCenter(polygon);
			label.setAlignment(Pos.CENTER);
			label.translateXProperty().bind(polygon.translateXProperty());
			label.translateYProperty().bind(polygon.translateYProperty());
			((Group) levelsGroup.getChildren().get(level)).getChildren().add(polygon);
			((Group) labelsGroup.getChildren().get(level)).getChildren().add(label);
			label.widthProperty().addListener((a, o, n) -> {
				if (o.doubleValue() == 0 && n.doubleValue() > 0) {
					label.setLayoutX(center.getX() - 0.5 * label.getWidth());
					label.setLayoutY(center.getY() - 0.5 * label.getHeight());
				}
			});
		};

		voronoiTreeMapService.setTask(rootNode, TreeNode::getChildren, TreeNode::getWeight, rootPolygon, resultConsumer);

		cancel.selectedProperty().addListener((v, o, n) -> {
			if (n)
				voronoiTreeMapService.cancel();
		});
		cancel.disableProperty().bind(cancel.selectedProperty());
		cancel.visibleProperty().bind(voronoiTreeMapService.runningProperty());

		voronoiTreeMapService.runningProperty().addListener((v, o, n) -> {
			cancel.setGraphic(n ? new ProgressIndicator() : null);
		});
		voronoiTreeMapService.setOnSucceeded(e -> {
			for (var i = 0; i < levelsGroup.getChildren().size(); i++) {
				var showLevel = new CheckBox("Show level " + i);
				showLevel.selectedProperty().bindBidirectional(levelsGroup.getChildren().get(i).visibleProperty());
				vbox.getChildren().add(showLevel);
				var showLabels = new CheckBox("Show labels " + i);
				showLabels.selectedProperty().bindBidirectional(labelsGroup.getChildren().get(i).visibleProperty());
				vbox.getChildren().add(showLabels);
			}
		});
		voronoiTreeMapService.start();

		outerGroup.setOnScroll(e -> {
			if (!voronoiTreeMapService.isRunning()) {
				if (e.getDeltaY() > 0 && zoom < 20) {
					zoom *= 1.1;
				} else if (e.getDeltaY() < 0 && zoom > 0.5) {
					zoom /= 1.1;
				} else
					return;
				outerGroup.setScaleX(zoom);
				outerGroup.setScaleY(zoom);
				for (var one : labelsGroup.getChildren()) {
					if (one instanceof Group group) {
						for (var node : group.getChildren()) {
							if (node instanceof Label label) {
								label.setScaleX(1 / zoom);
								label.setScaleY(1 / zoom);
							}
						}
					}
				}
			}
		});


	}

	private TreeNode setupTree() {
		var nameNodeMap = new TreeMap<String, TreeNode>();

		TreeNode.createLink(nameNodeMap, "Archaea", "GTDB");
		TreeNode.createLink(nameNodeMap, "Bacteria", "GTDB");
		TreeNode.createLink(nameNodeMap, "No Hits", "GTDB");
		TreeNode.createLink(nameNodeMap, "Not Assigned", "GTDB");

		TreeNode.createLink(nameNodeMap, "Aenigmatarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Altarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Asgardarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "EX4484-52", "Archaea");
		TreeNode.createLink(nameNodeMap, "Hadarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Halobacteriota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Huberarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Hydrothermarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Iainarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Methanobacteriota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Micrarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Nanoarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Nanohaloarchaeota", "Archaea");
		TreeNode.createLink(nameNodeMap, "PWEA01", "Archaea");
		TreeNode.createLink(nameNodeMap, "QMZS01", "Archaea");
		TreeNode.createLink(nameNodeMap, "Thermoplasmatota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Thermoproteota", "Archaea");
		TreeNode.createLink(nameNodeMap, "Undinarchaeota", "Archaea");

		TreeNode.createLink(nameNodeMap, "4572-55", "Bacteria");
		TreeNode.createLink(nameNodeMap, "AABM5-125-24", "Bacteria");
		TreeNode.createLink(nameNodeMap, "ARS69", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Abyssubacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Acidobacteriota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Actinobacteriota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Aerophobota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Aquificota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Armatimonadota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Aureabacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "B130-G9", "Bacteria");
		TreeNode.createLink(nameNodeMap, "B64-G9", "Bacteria");
		TreeNode.createLink(nameNodeMap, "BMS3Abin14", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Bacteroidota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Bdellovibrionota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Bdellovibrionota_C", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Bdellovibrionota_D", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Bipolaricaulota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "CG03", "Bacteria");
		TreeNode.createLink(nameNodeMap, "CG2-30-53-67", "Bacteria");
		TreeNode.createLink(nameNodeMap, "CG2-30-70-394", "Bacteria");
		TreeNode.createLink(nameNodeMap, "CSP1-3", "Bacteria");
		TreeNode.createLink(nameNodeMap, "CSSED10-310", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Caldatribacteriota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Caldisericota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Calditrichota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Calescibacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Campylobacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Chloroflexota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Chrysiogenetota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Cloacimonadota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Coprothermobacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Cyanobacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "DTU030", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Deferribacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Deinococcota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Delongbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Dependentiae", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Desantisbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Desulfobacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Desulfobacterota_B", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Desulfobacterota_C", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Desulfobacterota_D", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Dictyoglomota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Dormibacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Edwardsbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Eisenbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Elusimicrobiota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Eremiobacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "FCPU426", "Bacteria");
		TreeNode.createLink(nameNodeMap, "FEN-1099", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Fermentibacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Fibrobacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firestonebacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes_A", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes_B", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes_C", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes_D", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes_E", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes_F", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Firmicutes_G", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Fusobacteriota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "GCA-001730085", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Gemmatimonadota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Gemmatimonadota_A", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Goldbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Hydrogenedentota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "J088", "Bacteria");
		TreeNode.createLink(nameNodeMap, "KSB1", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Krumholzibacteriota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Latescibacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Lindowbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Margulisbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Marinisomatota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Methylomirabilota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Moduliflexota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Muirbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Myxococcota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Myxococcota_A", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Myxococcota_B", "Bacteria");
		TreeNode.createLink(nameNodeMap, "NPL-UPA2", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Nitrospinota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Nitrospinota_A", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Nitrospinota_B", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Nitrospirota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "OLB16", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Omnitrophota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Patescibacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Planctomycetota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Poribacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Proteobacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "RBG-13-66-14", "Bacteria");
		TreeNode.createLink(nameNodeMap, "RUG730", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Riflebacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "SAR324", "Bacteria");
		TreeNode.createLink(nameNodeMap, "SLNR01", "Bacteria");
		TreeNode.createLink(nameNodeMap, "SM23-31", "Bacteria");
		TreeNode.createLink(nameNodeMap, "SURF-8", "Bacteria");
		TreeNode.createLink(nameNodeMap, "SZUA-182", "Bacteria");
		TreeNode.createLink(nameNodeMap, "SZUA-79", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Schekmanbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Spirochaetota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Sumerlaeota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Synergistota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "T1SED10-198M", "Bacteria");
		TreeNode.createLink(nameNodeMap, "TA06", "Bacteria");
		TreeNode.createLink(nameNodeMap, "TA06_A", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Tectomicrobia", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Thermodesulfobiota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Thermosulfidibacterota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Thermotogota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBA10199", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBA2233", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBA3054", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBA6262", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBA9089", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP13", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP14", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP15", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP17", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP18", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP4", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP6", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP7", "Bacteria");
		TreeNode.createLink(nameNodeMap, "UBP7_A", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Verrucomicrobiota", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Verrucomicrobiota_A", "Bacteria");
		TreeNode.createLink(nameNodeMap, "WOR-3", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Wallbacteria", "Bacteria");
		TreeNode.createLink(nameNodeMap, "Zixibacteria", "Bacteria");
		return nameNodeMap.get("GTDB");
	}

	private final Map<Integer, Font> layerFontMap = new ConcurrentHashMap<>();

	public Font layerToFont(int layer) {
		return switch (layer) {
			case 0 -> layerFontMap.computeIfAbsent(layer, k -> Font.font("System", 16));
			case 1 -> layerFontMap.computeIfAbsent(layer, k -> Font.font("System", 12));
			case 2 -> layerFontMap.computeIfAbsent(layer, k -> Font.font("System", 8));
			default -> layerFontMap.computeIfAbsent(layer, k -> Font.font("System", 6));
		};
	}

}
