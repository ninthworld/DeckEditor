package org.ninthworld.deckeditor;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    private HashMap<String, Color> rarityColor;
    private HashMap<String, Image> symbols;

    private ArrayList<SetData> allSets;
    private ArrayList<CardData> allCards;

    private ArrayList<String> expansionBlockOrder;
    private HashMap<String, ArrayList<SetData>> expansionSets;
    private ArrayList<SetData> coreSets, commanderSets;

    private ArrayList<CheckBox> searchCostCB, searchManaCB, searchRarityCB, searchTypeCB;
    private ArrayList<CardData> searchCardList;

    private ListView searchListView;
    private TextArea cardInfoTextArea;
    private ImageView cardPreview;
    private Image cardBack;
    private HashMap<String, Image> cardPreviewCache;

    private TextField searchTextBox;

    private ArrayList<HashMap<CardData, Integer>> deckCardMap; // commanderCardList, mainboardCardList, sideboardCardList, maybeboardCardList;
    private ArrayList<ListView> deckListView; // commanderListView, mainboardListView, sideboardListView, maybeboardListView;
    private TabPane deckTabPane;

    private Button centerPlusButton, centerMinusButton, centerAddButton, centerRemoveButton;

    private TextField deckNameTextField;
    private Button newButton, openButton, saveButton, importButton, exportButton;

    private StackedBarChart manaCurveChart;
    private PieChart typePieChart, costManaPieChart;

    private File deckFile = null;

    private String titlePrefix = "MagicFX - Deck Editor - ";
    private boolean isSaved = true;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("deckeditor.fxml"));
        primaryStage.setTitle(titlePrefix + "Untitled [*]");
        primaryStage.setScene(new Scene(root, 1280, 800));
        primaryStage.show();

        Scene scene = primaryStage.getScene();

        cardInfoTextArea = (TextArea) scene.lookup("#cardInfoTextArea");
        cardPreview = (ImageView) scene.lookup("#cardPreview");
        cardBack = new Image(getClass().getResource("/symbols/back.jpg").toString());
        cardPreview.setImage(cardBack);
        cardPreview.fitWidthProperty().bind(((GridPane) cardPreview.getParent()).widthProperty());
        cardPreview.fitHeightProperty().bind(((GridPane) cardPreview.getParent()).heightProperty());
        cardPreviewCache = new HashMap<>();

        manaCurveChart = (StackedBarChart) scene.lookup("#manaCurveChart");
        typePieChart = (PieChart) scene.lookup("#typePieChart");
        costManaPieChart = (PieChart) scene.lookup("#costManaPieChart");

        // Load Set Data
        SetData.loadAllSets(getClass().getResourceAsStream("/json/AllSets.json"), allSets = new ArrayList<SetData>(), allCards = new ArrayList<CardData>());
        Collections.sort(allCards, (card1, card2) -> card1.getName().compareTo(card2.getName()));

        // Sort Expansion Sets
        expansionSets = new HashMap<>();
        allSets.forEach(setData->{
            if(setData.getType().equalsIgnoreCase("expansion")){
                if(!expansionSets.containsKey(setData.getBlock())){
                    expansionSets.put(setData.getBlock(), new ArrayList<>());
                }
                expansionSets.get(setData.getBlock()).add(setData);
            }
        });

        expansionSets.keySet().forEach(blockName->{
            Collections.sort(expansionSets.get(blockName), (set1, set2) -> {
                if(set1.isDateOlder(set2)){
                    return 1;
                }
                return -1;
            });
        });

        expansionBlockOrder = new ArrayList<>();
        expansionSets.keySet().forEach(blockName-> expansionBlockOrder.add(blockName));
        Collections.sort(expansionBlockOrder, (block1, block2) -> {
           if(expansionSets.get(block1).get(0).isDateOlder(expansionSets.get(block2).get(0))){
                return 1;
            }
            return -1;
        });

        // Sort Core Sets
        coreSets = new ArrayList<>();
        allSets.forEach(setData -> {
            if(setData.getType().equalsIgnoreCase("core")){
                coreSets.add(setData);
            }
        });
        Collections.sort(coreSets, (core1, core2) -> {
            if(core1.isDateOlder(core2)){
                return 1;
            }
            return -1;
        });

        // Sort Commander
        commanderSets = new ArrayList<>();
        allSets.forEach(setData -> {
            if(setData.getType().equalsIgnoreCase("commander")){
                commanderSets.add(setData);
            }
        });
        Collections.sort(commanderSets, (cmd1, cmd2) -> {
            if(cmd1.isDateOlder(cmd2)){
                return 1;
            }
            return -1;
        });

        // Load All Symbols
        symbols = new HashMap<>();
        loadAllImages(symbols, "/symbols/");

        // Rarity Colors
        rarityColor = new HashMap<>();
        rarityColor.put("Common", Color.valueOf("#242021"));
        rarityColor.put("Uncommon", Color.valueOf("#458aa0"));
        rarityColor.put("Rare", Color.valueOf("#ab8d39"));
        rarityColor.put("Mythic Rare", Color.valueOf("#cc5e33"));

        // Deck Area
        deckTabPane = (TabPane) scene.lookup("#deckTabPane");
        deckTabPane.getSelectionModel().select(1);

        deckListView = new ArrayList<>();
        deckListView.add((ListView) scene.lookup("#commanderListView"));
        deckListView.add((ListView) scene.lookup("#mainboardListView"));
        deckListView.add((ListView) scene.lookup("#sideboardListView"));
        deckListView.add((ListView) scene.lookup("#maybeboardListView"));

        deckCardMap = new ArrayList<>();
        deckCardMap.add(new HashMap<>());
        deckCardMap.add(new HashMap<>());
        deckCardMap.add(new HashMap<>());
        deckCardMap.add(new HashMap<>());

        // Card Search
        // Sets
        VBox searchSetsVBox = (VBox) scene.lookup("#searchSetsVBox");

        // Expansion Sets
        Label expansionsLabel = new Label("Expansion Sets");
        expansionsLabel.setPadding(new Insets(12, 0, 0, 0));
        expansionsLabel.setFont(Font.font(null, FontWeight.BOLD, 16));
        searchSetsVBox.getChildren().add(expansionsLabel);
        expansionBlockOrder.forEach(blockName->{
            Label blockLabel = new Label(blockName);
            blockLabel.setPadding(new Insets(6, 0, 0, 12));
            searchSetsVBox.getChildren().add(blockLabel);

            VBox inner = new VBox();
            inner.setPadding(new Insets(4, 8, 4, 12));
            inner.setSpacing(4);
            searchSetsVBox.getChildren().add(inner);
            expansionSets.get(blockName).forEach(setData->{
                CheckBox box = new CheckBox();
                box.setText(setData.getName());
                box.setPadding(new Insets(0, 0, 0, 0));

                inner.getChildren().add(box);
            });
        });

        // Core Sets
        Label coreLabel = new Label("Core Sets");
        coreLabel.setPadding(new Insets(12, 0, 0, 0));
        coreLabel.setFont(Font.font(null, FontWeight.BOLD, 16));
        searchSetsVBox.getChildren().add(coreLabel);

        VBox coreInner = new VBox();
        coreInner.setPadding(new Insets(4, 8, 4, 12));
        coreInner.setSpacing(4);
        searchSetsVBox.getChildren().add(coreInner);
        coreSets.forEach(setData->{
            CheckBox box = new CheckBox();
            box.setText(setData.getName());

            coreInner.getChildren().add(box);
        });

        // Commander Sets
        Label commanderLabel = new Label("Commander Sets");
        commanderLabel.setPadding(new Insets(12, 0, 0, 0));
        commanderLabel.setFont(Font.font(null, FontWeight.BOLD, 16));
        searchSetsVBox.getChildren().add(commanderLabel);

        VBox commanderInner = new VBox();
        commanderInner.setPadding(new Insets(4, 8, 4, 12));
        commanderInner.setSpacing(4);
        searchSetsVBox.getChildren().add(commanderInner);
        commanderSets.forEach(setData->{
            CheckBox box = new CheckBox();
            box.setText(setData.getName());

            commanderInner.getChildren().add(box);
        });

        CheckBox searchSetsSelectAllCheck = (CheckBox) scene.lookup("#searchSetsSelectAllCheck");

        ((VBox) searchSetsSelectAllCheck.getParent()).getChildren().forEach(child -> {
            if(child instanceof VBox){
                ((VBox) child).getChildren().forEach(childchild -> {
                    if(childchild instanceof CheckBox){
                        ((CheckBox) childchild).setOnAction(e -> {
                            updateSearchCardList(searchCardList, scene);
                            updateCardListView(searchListView, searchCardList);
                        });
                    }
                });
            }
        });

        searchSetsSelectAllCheck.setOnAction(e->{
            boolean selected = searchSetsSelectAllCheck.isSelected();
            ((VBox) searchSetsSelectAllCheck.getParent()).getChildren().forEach(child->{
                if(child instanceof VBox){
                    VBox vbox = (VBox) child;
                    vbox.getChildren().forEach(checkChild->{
                        if(checkChild instanceof CheckBox){
                            CheckBox box = (CheckBox) checkChild;
                            box.setSelected(selected);
                        }
                    });
                }
            });

            updateSearchCardList(searchCardList, scene);
            updateCardListView(searchListView, searchCardList);
        });

        CheckBox searchSetsLatestFiveCheck = (CheckBox) scene.lookup("#searchSetsLatestFiveCheck");
        searchSetsLatestFiveCheck.setOnAction(e->{
            boolean selected = searchSetsLatestFiveCheck.isSelected();
            int blockCount = 5;
            for(Node child : ((VBox) searchSetsSelectAllCheck.getParent()).getChildren()){
                if(child instanceof VBox){
                    VBox block = (VBox) child;
                    if(blockCount-- > 0){
                        block.getChildren().forEach(childchild->{
                            if(childchild instanceof CheckBox){
                                ((CheckBox) childchild).setSelected(selected);
                            }
                        });
                    }
                }
            }

            updateSearchCardList(searchCardList, scene);
            updateCardListView(searchListView, searchCardList);
        });

        // Types
        CheckBox searchTypeSelectAllCheck = (CheckBox) scene.lookup("#searchTypeSelectAllCheck");
        searchTypeCB = new ArrayList<>();
        searchTypeCB.add((CheckBox) scene.lookup("#searchTypeInstantCheck"));
        searchTypeCB.add((CheckBox) scene.lookup("#searchTypeSorceryCheck"));
        searchTypeCB.add((CheckBox) scene.lookup("#searchTypeCreatureCheck"));
        searchTypeCB.add((CheckBox) scene.lookup("#searchTypeArtifactCheck"));
        searchTypeCB.add((CheckBox) scene.lookup("#searchTypeEnchantmentCheck"));
        searchTypeCB.add((CheckBox) scene.lookup("#searchTypePlaneswalkerCheck"));
        searchTypeCB.add((CheckBox) scene.lookup("#searchTypeLandCheck"));

        searchTypeCB.get(0).setGraphic(new ImageView(symbols.get("Instant")));
        searchTypeCB.get(1).setGraphic(new ImageView(symbols.get("Sorcery")));
        searchTypeCB.get(2).setGraphic(new ImageView(symbols.get("Creature")));
        searchTypeCB.get(3).setGraphic(new ImageView(symbols.get("Artifact")));
        searchTypeCB.get(4).setGraphic(new ImageView(symbols.get("Enchantment")));
        searchTypeCB.get(5).setGraphic(new ImageView(symbols.get("Planeswalker")));
        searchTypeCB.get(6).setGraphic(new ImageView(symbols.get("Land")));

        ((VBox) searchTypeSelectAllCheck.getParent()).getChildren().forEach(child -> {
            if(child instanceof CheckBox){
                ((CheckBox) child).setOnAction(e -> {
                    updateSearchCardList(searchCardList, scene);
                    updateCardListView(searchListView, searchCardList);
                });
            }
        });

        searchTypeSelectAllCheck.setOnAction(e->{
            boolean selected = searchTypeSelectAllCheck.isSelected();
            searchTypeSelectAllCheck.getParent().getChildrenUnmodifiable().forEach(child->{
                if(child instanceof CheckBox){
                    CheckBox box = (CheckBox) child;
                    box.setSelected(selected);
                }
            });

            updateSearchCardList(searchCardList, scene);
            updateCardListView(searchListView, searchCardList);
        });

        // Rarity
        CheckBox searchRaritySelectAllCheck = (CheckBox) scene.lookup("#searchRaritySelectAllCheck");
        searchRarityCB = new ArrayList<>();
        searchRarityCB.add((CheckBox) scene.lookup("#searchRarityCommonCheck"));
        searchRarityCB.add((CheckBox) scene.lookup("#searchRarityUncommonCheck"));
        searchRarityCB.add((CheckBox) scene.lookup("#searchRarityRareCheck"));
        searchRarityCB.add((CheckBox) scene.lookup("#searchRarityMythicCheck"));

        searchRarityCB.get(0).setGraphic(new ImageView(symbols.get("Common")));
        searchRarityCB.get(1).setGraphic(new ImageView(symbols.get("Uncommon")));
        searchRarityCB.get(2).setGraphic(new ImageView(symbols.get("Rare")));
        searchRarityCB.get(3).setGraphic(new ImageView(symbols.get("Mythic Rare")));

        ((VBox) searchRaritySelectAllCheck.getParent()).getChildren().forEach(child -> {
            if(child instanceof CheckBox){
                ((CheckBox) child).setOnAction(e -> {
                    updateSearchCardList(searchCardList, scene);
                    updateCardListView(searchListView, searchCardList);
                });
            }
        });

        searchRaritySelectAllCheck.setOnAction(e->{
            boolean selected = searchRaritySelectAllCheck.isSelected();
            searchRaritySelectAllCheck.getParent().getChildrenUnmodifiable().forEach(child->{
                if(child instanceof CheckBox){
                    CheckBox box = (CheckBox) child;
                    box.setSelected(selected);
                }
            });

            updateSearchCardList(searchCardList, scene);
            updateCardListView(searchListView, searchCardList);
        });

        // Colors
        CheckBox searchManaSelectAllCheck = (CheckBox) scene.lookup("#searchManaSelectAllCheck");
        searchManaCB = new ArrayList<>();
        searchManaCB.add((CheckBox) scene.lookup("#searchManaColorlessCheck"));
        searchManaCB.add((CheckBox) scene.lookup("#searchManaGreenCheck"));
        searchManaCB.add((CheckBox) scene.lookup("#searchManaBlueCheck"));
        searchManaCB.add((CheckBox) scene.lookup("#searchManaRedCheck"));
        searchManaCB.add((CheckBox) scene.lookup("#searchManaBlackCheck"));
        searchManaCB.add((CheckBox) scene.lookup("#searchManaWhiteCheck"));
        searchManaCB.add((CheckBox) scene.lookup("#searchManaMulticoloredCheck"));

        searchManaCB.get(0).setGraphic(new ImageView(symbols.get("C")));
        searchManaCB.get(1).setGraphic(new ImageView(symbols.get("G")));
        searchManaCB.get(2).setGraphic(new ImageView(symbols.get("U")));
        searchManaCB.get(3).setGraphic(new ImageView(symbols.get("R")));
        searchManaCB.get(4).setGraphic(new ImageView(symbols.get("B")));
        searchManaCB.get(5).setGraphic(new ImageView(symbols.get("W")));
        searchManaCB.get(6).setGraphic(new ImageView(symbols.get("GU")));

        ((VBox) searchManaSelectAllCheck.getParent()).getChildren().forEach(child -> {
            if(child instanceof CheckBox){
                ((CheckBox) child).setOnAction(e -> {
                    updateSearchCardList(searchCardList, scene);
                    updateCardListView(searchListView, searchCardList);
                });
            }
        });

        searchManaSelectAllCheck.setOnAction(e->{
            boolean selected = searchManaSelectAllCheck.isSelected();
            searchManaSelectAllCheck.getParent().getChildrenUnmodifiable().forEach(child->{
                if(child instanceof CheckBox && child != searchManaCB.get(6)){
                    CheckBox box = (CheckBox) child;
                    box.setSelected(selected);
                }
            });

            updateSearchCardList(searchCardList, scene);
            updateCardListView(searchListView, searchCardList);
        });

        // Cost
        CheckBox searchCostSelectAllCheck = (CheckBox) scene.lookup("#searchCostSelectAllCheck");
        searchCostCB = new ArrayList<>();
        searchCostCB.add((CheckBox) scene.lookup("#searchCostZeroCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostOneCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostTwoCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostThreeCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostFourCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostFiveCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostSixCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostSevenCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostEightCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostNineCheck"));
        searchCostCB.add((CheckBox) scene.lookup("#searchCostTenOrMoreCheck"));

        searchCostCB.get(0).setGraphic(new ImageView(symbols.get("0")));
        searchCostCB.get(1).setGraphic(new ImageView(symbols.get("1")));
        searchCostCB.get(2).setGraphic(new ImageView(symbols.get("2")));
        searchCostCB.get(3).setGraphic(new ImageView(symbols.get("3")));
        searchCostCB.get(4).setGraphic(new ImageView(symbols.get("4")));
        searchCostCB.get(5).setGraphic(new ImageView(symbols.get("5")));
        searchCostCB.get(6).setGraphic(new ImageView(symbols.get("6")));
        searchCostCB.get(7).setGraphic(new ImageView(symbols.get("7")));
        searchCostCB.get(8).setGraphic(new ImageView(symbols.get("8")));
        searchCostCB.get(9).setGraphic(new ImageView(symbols.get("9")));
        searchCostCB.get(10).setGraphic(new ImageView(symbols.get("10")));

        ((VBox) searchCostSelectAllCheck.getParent()).getChildren().forEach(child -> {
            if(child instanceof CheckBox){
                ((CheckBox) child).setOnAction(e -> {
                    updateSearchCardList(searchCardList, scene);
                    updateCardListView(searchListView, searchCardList);
                });
            }
        });

        searchCostSelectAllCheck.setOnAction(e->{
            boolean selected = searchCostSelectAllCheck.isSelected();
            searchCostSelectAllCheck.getParent().getChildrenUnmodifiable().forEach(child->{
                if(child instanceof CheckBox){
                    CheckBox box = (CheckBox) child;
                    box.setSelected(selected);
                }
            });

            updateSearchCardList(searchCardList, scene);
            updateCardListView(searchListView, searchCardList);
        });

        // Cards
        searchCardList = new ArrayList<>();
        searchListView = (ListView) scene.lookup("#searchListView");

        searchListView.setOnMouseClicked(e -> {
            updateCardPreview(searchListView, searchCardList);

            if(e.getClickCount() == 2){
                int tabIndex = deckTabPane.getSelectionModel().getSelectedIndex();
                addCardToDeckList(searchListView, deckListView.get(tabIndex), searchCardList, deckCardMap.get(tabIndex), primaryStage);
            }
        });
        searchListView.setOnKeyReleased(e -> updateCardPreview(searchListView, searchCardList));
        searchListView.setOnKeyPressed(e -> {
            if(e.getCode() == KeyCode.ENTER){
                int tabIndex = deckTabPane.getSelectionModel().getSelectedIndex();
                addCardToDeckList(searchListView, deckListView.get(tabIndex), searchCardList, deckCardMap.get(tabIndex), primaryStage);
            }
        });

        searchTextBox = (TextField) scene.lookup("#searchTextBox");
        searchTextBox.setOnAction(e -> {
            //updateSearchCardList(searchCardList, scene);
            //updateCardListView(searchListView, searchCardList);
            int tabIndex = deckTabPane.getSelectionModel().getSelectedIndex();
            addCardToDeckList(searchListView, deckListView.get(tabIndex), searchCardList, deckCardMap.get(tabIndex), primaryStage);
        });

        searchTextBox.setOnKeyReleased(e -> {
            updateSearchCardList(searchCardList, scene);
            updateCardListView(searchListView, searchCardList);
        });

        searchSetsLatestFiveCheck.fire();

        // Deck ListViews
        for(int i=0; i<deckListView.size(); i++){
            int finalI = i;
            deckListView.get(i).setOnMouseClicked(e -> {
                if(e.getButton() == MouseButton.PRIMARY) {
                    updateCardPreview(deckListView.get(finalI), deckCardMap.get(finalI));

                    if(e.getClickCount() == 2){
                        removeCardFromDeckList(deckListView.get(finalI), deckCardMap.get(finalI), false, primaryStage);
                    }
                }
            });

            deckListView.get(i).setOnKeyReleased(e -> {
                if(e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE){
                    removeCardFromDeckList(deckListView.get(finalI), deckCardMap.get(finalI), true, primaryStage);
                }else{
                    updateCardPreview(deckListView.get(finalI), deckCardMap.get(finalI));
                }
            });
        }

        // Center Control Buttons
        centerPlusButton = (Button) scene.lookup("#centerPlusButton");
        centerMinusButton = (Button) scene.lookup("#centerMinusButton");
        centerAddButton = (Button) scene.lookup("#centerAddButton");
        centerRemoveButton = (Button) scene.lookup("#centerRemoveButton");

        centerPlusButton.setGraphic(new ImageView(symbols.get("iconPlus")));
        centerMinusButton.setGraphic(new ImageView(symbols.get("iconMinus")));
        centerAddButton.setGraphic(new ImageView(symbols.get("iconAdd")));
        centerRemoveButton.setGraphic(new ImageView(symbols.get("iconRemove")));

        centerPlusButton.setOnAction(e -> {
            int tabIndex = deckTabPane.getSelectionModel().getSelectedIndex();
            int selectedIndex = deckListView.get(tabIndex).getSelectionModel().getSelectedIndex();
            addCardToDeckList(deckListView.get(tabIndex), deckListView.get(tabIndex), getSortedDeck(deckCardMap.get(tabIndex)), deckCardMap.get(tabIndex), primaryStage);
            updateDeckCardListView(deckListView.get(tabIndex), deckCardMap.get(tabIndex), selectedIndex);
        });

        centerMinusButton.setOnAction(e -> {
            int tabIndex = deckTabPane.getSelectionModel().getSelectedIndex();
            removeCardFromDeckList(deckListView.get(tabIndex), deckCardMap.get(tabIndex), false, primaryStage);
        });

        centerAddButton.setOnAction(e -> {
            int tabIndex = deckTabPane.getSelectionModel().getSelectedIndex();
            addCardToDeckList(searchListView, deckListView.get(tabIndex), searchCardList, deckCardMap.get(tabIndex), primaryStage);
        });

        centerRemoveButton.setOnAction(e -> {
            int tabIndex = deckTabPane.getSelectionModel().getSelectedIndex();
            removeCardFromDeckList(deckListView.get(tabIndex), deckCardMap.get(tabIndex), true, primaryStage);
        });

        // Main Controls
        deckNameTextField = (TextField) scene.lookup("#deckNameTextField");
        deckNameTextField.setText("Untitled");

        newButton = (Button) scene.lookup("#newButton");
        saveButton = (Button) scene.lookup("#saveButton");
        openButton = (Button) scene.lookup("#openButton");
        importButton = (Button) scene.lookup("#importButton");
        exportButton = (Button) scene.lookup("#exportButton");

        newButton.setOnAction(e -> actionNewDeck(primaryStage));
        saveButton.setOnAction(e -> actionSaveDeck(primaryStage));
        openButton.setOnAction(e -> actionOpenDeck(primaryStage));
        importButton.setOnAction(e -> actionImportDeck(primaryStage));
        exportButton.setOnAction(e -> actionExportDeck(primaryStage));

        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            closeWindow(primaryStage);
        });

        // Menu Items
        MenuBar menuBar = (MenuBar) scene.lookup("#menuBar");
        menuBar.getMenus().get(0).getItems().get(0).setOnAction(e -> closeWindow(primaryStage));

    }

    private void closeWindow(Stage primaryStage){
        if(!isSaved) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Save Changes?");
            alert.setHeaderText("The deck has been modified.");
            alert.setContentText("Do you want to save changes?");

            ButtonType saveBtn = new ButtonType("Save");
            ButtonType discardBtn = new ButtonType("Discard");
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

            Optional<ButtonType> result = alert.showAndWait();
            if(result.get() == saveBtn){
                actionSaveDeck(primaryStage);
                primaryStage.close();
            }else if(result.get() == discardBtn){
                primaryStage.close();
            }else{
                alert.close();
            }
        }else{
            primaryStage.close();
        }
    }

    private void actionNewDeck(Stage primaryStage){
        deckFile = null;
        deckNameTextField.setText("Untitled");
        for(int i=0; i<deckCardMap.size(); i++){
            deckCardMap.get(i).clear();
            updateDeckCardListView(deckListView.get(i), deckCardMap.get(i));
        }

        setSaved(false, primaryStage);
    }

    private void actionSaveDeck(Stage primaryStage){
        if(deckFile == null){
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(deckNameTextField.getText() + ".jdeck");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JDECK", "*.jdeck")
            );
            deckFile = fileChooser.showSaveDialog(primaryStage);
        }

        if(deckFile != null){
            JSONObject deckObj = new JSONObject();
            deckObj.put("name", deckNameTextField.getText());

            for(int i=0; i<deckCardMap.size(); i++){
                JSONArray cardsArray = new JSONArray();
                for(CardData cardData : deckCardMap.get(i).keySet()){
                    JSONObject cardObj = new JSONObject();

                    cardObj.put("count", deckCardMap.get(i).get(cardData));
                    cardObj.put("multiverseId", cardData.getMultiverseId());

                    cardsArray.add(cardObj);
                }

                deckObj.put("section" + i, cardsArray);
            }

            try {
                FileWriter fw = new FileWriter(deckFile);
                fw.write(deckObj.toJSONString());
                fw.flush();
                fw.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            setSaved(true, primaryStage);
        }
    }

    private void actionOpenDeck(Stage primaryStage){
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JDECK", "*.jdeck")
        );
        deckFile = fileChooser.showOpenDialog(primaryStage);

        if(deckFile != null) {
            try {
                JSONParser parser = new JSONParser();
                JSONObject deckObj = (JSONObject) parser.parse(new FileReader(deckFile));

                if (deckObj.containsKey("name")) {
                    deckNameTextField.setText(deckObj.get("name").toString());
                }

                for(int i=0; i<deckCardMap.size(); i++){
                    if (deckObj.containsKey("section" + i)) {
                        JSONArray cardsArray = (JSONArray) deckObj.get("section" + i);
                        for(Object card : cardsArray){
                            JSONObject cardObj = (JSONObject) card;
                            int count = 1;

                            if (cardObj.containsKey("count")) {
                                count = Integer.parseInt(cardObj.get("count").toString());
                            }

                            if (cardObj.containsKey("multiverseId")) {
                                for (CardData cardData : allCards) {
                                    if (cardData.getMultiverseId().equals(cardObj.get("multiverseId").toString())) {
                                        deckCardMap.get(i).put(cardData, count);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    updateDeckCardListView(deckListView.get(i), deckCardMap.get(i));
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ParseException e1) {
                e1.printStackTrace();
            }

            updateCharts();
            setSaved(true, primaryStage);
        }
    }

    private void actionImportDeck(Stage primaryStage){

    }

    private void actionExportDeck(Stage primaryStage){

    }

    private void setSaved(boolean val, Stage primaryStage){
        isSaved = val;

        primaryStage.setTitle(titlePrefix + deckNameTextField.getText() + (!isSaved ? " [*]" : ""));
    }

    private void addCardToDeckList(ListView srcListView, ListView destListView, ArrayList<CardData> srcCardData, HashMap<CardData, Integer> destCardData, Stage primaryStage){
        BorderPane borderPane = (BorderPane) srcListView.getSelectionModel().getSelectedItem();
        if(borderPane != null) {
            int searchCardListIndex = Integer.parseInt(((Label) borderPane.getCenter()).getText());
            CardData card = srcCardData.get(searchCardListIndex);

            if(destCardData.containsKey(card)){
                destCardData.put(card, destCardData.get(card)+1);
            }else{
                destCardData.put(card, 1);
            }

            updateDeckCardListView(destListView, destCardData);
        }

        updateCharts();
        setSaved(false, primaryStage);
    }

    private void removeCardFromDeckList(ListView listView, HashMap<CardData, Integer> cards, boolean allFlag, Stage primaryStage){
        BorderPane borderPane = (BorderPane) listView.getSelectionModel().getSelectedItem();
        if(borderPane != null) {
            ArrayList<CardData> cardList = getSortedDeck(cards);

            int searchCardListIndex = Integer.parseInt(((Label) borderPane.getCenter()).getText());
            CardData card = cardList.get(searchCardListIndex);

            if(cards.containsKey(card)){
                int count = cards.get(card);
                if(count > 1 && !allFlag){
                    cards.put(card, --count);
                }else{
                    cards.remove(card);
                    searchCardListIndex--;
                }
            }

            updateDeckCardListView(listView, cards, searchCardListIndex);
        }

        updateCharts();
        setSaved(false, primaryStage);
    }

    private void updateCardInfo(CardData card) {
        cardInfoTextArea.setText(Arrays.toString(card.getColorIdentity()));
    }

    private void updateCharts(){
        int[] typeCounts = new int[7];

        deckCardMap.get(1).keySet().forEach(cardData -> {
            if(cardData.getTypes() != null) {
                Arrays.asList(cardData.getTypes()).forEach(type -> {
                    switch(type.toLowerCase()){
                        case "instant":
                            typeCounts[0]++;
                            break;
                        case "sorcery":
                            typeCounts[1]++;
                            break;
                        case "creature":
                            typeCounts[2]++;
                            break;
                        case "artifact":
                            typeCounts[3]++;
                            break;
                        case "enchantment":
                            typeCounts[4]++;
                            break;
                        case "planeswalker":
                            typeCounts[5]++;
                            break;
                        case "land":
                            typeCounts[6]++;
                            break;
                    }
                });
            }
        });

        typePieChart.setLegendVisible(false);
        typePieChart.setData(
                FXCollections.observableArrayList(
                        new PieChart.Data("Instant", typeCounts[0]),
                        new PieChart.Data("Sorcery", typeCounts[1]),
                        new PieChart.Data("Creature", typeCounts[2]),
                        new PieChart.Data("Artifact", typeCounts[3]),
                        new PieChart.Data("Enchantment", typeCounts[4]),
                        new PieChart.Data("Planeswalker", typeCounts[5]),
                        new PieChart.Data("Land", typeCounts[6])
                )
        );

        String[] colors = new String[]{
                "#4572a7",
                "#b21c9b",
                "#80699b",
                "#89a54e",
                "#aa4643",
                "#e3e53f",
                "#3d96ae"
        };

        int i = 0;
        for(PieChart.Data data : typePieChart.getData()){
            data.getNode().setStyle("-fx-pie-color: " + colors[i%colors.length] + ";");
            i++;
        }
    }

    private ArrayList<CardData> getSortedDeck(HashMap<CardData, Integer> cards){
        ArrayList<CardData> cardList = new ArrayList<>();
        cards.keySet().forEach(cardData -> cardList.add(cardData));
        Collections.sort(cardList, (card1, card2) -> card1.getName().compareTo(card2.getName()));

        return cardList;
    }

    private void updateCardPreview(ListView listView, HashMap<CardData, Integer> cards){
        updateCardPreview(listView, getSortedDeck(cards));
    }

    private void updateCardPreview(ListView listView, ArrayList<CardData> cards) {
        BorderPane borderPane = (BorderPane) listView.getSelectionModel().getSelectedItem();
        if(borderPane != null) {
            int searchCardListIndex = Integer.parseInt(((Label) borderPane.getCenter()).getText());
            CardData card = cards.get(searchCardListIndex);
            updateCardInfo(card);

            if(!cardPreviewCache.containsKey(card.getMultiverseId())) {
                File hqCard = new File("res/cache/hqcards/" + card.getSetCode() + "/" + card.getName().replaceAll("\\\"", "") + (card.hasVariations() ? card.getVariationNum() : "") + ".full.jpg");
                if (!hqCard.exists()) {
                    File lqCard = new File("res/cache/lqcards/" + card.getSetCode() + "/" + card.getName().replaceAll("\\\"", "") + (card.hasVariations() ? card.getVariationNum() : "") + ".full.jpg");
                    if (!lqCard.exists()) {
                        // Download
                        try {
                            URL url = new URL("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=" + card.getMultiverseId() + "&type=card");
                            InputStream in = new BufferedInputStream(url.openStream());
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            int n = 0;
                            while((n = in.read(buf)) != -1){
                                out.write(buf, 0, n);
                            }
                            out.close();
                            in.close();

                            new File(lqCard.getAbsolutePath().substring(0, lqCard.getAbsolutePath().lastIndexOf(File.separator))).mkdirs();
                            FileOutputStream fos = new FileOutputStream(lqCard);
                            fos.write(out.toByteArray());
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    cardPreviewCache.put(card.getMultiverseId(), new Image(lqCard.toURI().toString()));
                } else {
                    cardPreviewCache.put(card.getMultiverseId(), new Image(hqCard.toURI().toString()));
                }
            }

            if(cardPreviewCache.containsKey(card.getMultiverseId())) {
                Image cardImg = cardPreviewCache.get(card.getMultiverseId());
                cardPreview.setImage(cardImg);
            }else{
                cardPreview.setImage(cardBack);
            }
        }
    }

    private void updateSearchCardList(ArrayList<CardData> cards, Scene scene){
        cards.clear();

        ArrayList<String> setCodes = new ArrayList<>();
        VBox sets = (VBox) scene.lookup("#searchSetsVBox");
        sets.getChildren().forEach(setChild -> {
            if(setChild instanceof VBox){
                VBox child = (VBox) setChild;
                child.getChildren().forEach(setName -> {
                    if(setName instanceof CheckBox){
                        CheckBox checkBox = (CheckBox) setName;
                        if(checkBox.isSelected()) {
                            String name = checkBox.getText();
                            for (SetData setData : allSets) {
                                if (setData.getBlock().equalsIgnoreCase(name) || setData.getName().equalsIgnoreCase(name)) {
                                    setCodes.add(setData.getCode());
                                }
                            }
                        }
                    }
                });
            }
        });

        CardDataLoop:
        for(CardData cardData : allCards){
            String cardName = "";
            if(cardData.getNames() != null && cardData.getNames().length > 1){
                for(int i=0; i<cardData.getNames().length; i++){
                    cardName += cardData.getNames()[i];
                    if(i < cardData.getNames().length-1){
                        cardName += " ";
                    }
                }
            }else {
                cardName = cardData.getName();
            }


            if(!cardName.toLowerCase().contains(searchTextBox.getText().toLowerCase())){
                continue CardDataLoop;
            }

            if(cardData.getCmc() > 10){
                if(!searchCostCB.get(10).isSelected()){
                    continue CardDataLoop;
                }
            }else if(!searchCostCB.get(cardData.getCmc()).isSelected()){
                continue CardDataLoop;
            }

            List<String> colors = new ArrayList<>();
            String[] colorCode = new String[]{"C", "G", "U", "R", "B", "W"};
            if(cardData.getColorIdentity() == null){
                colors.add("C");
            }else{
                colors.addAll(Arrays.asList(cardData.getColorIdentity()));
            }

            for(int i=0; i<searchManaCB.size(); i++){
                if(i >= searchManaCB.size()-1){
                    if(colors.size() <= 1 && searchManaCB.get(i).isSelected()){
                        continue CardDataLoop;
                    }
                }else {
                    if (colors.contains(colorCode[i]) && !searchManaCB.get(i).isSelected()) {
                        continue CardDataLoop;
                    }
                }
            }

            for(int i=0; i<searchRarityCB.size(); i++){
                if((cardData.getRarity().equalsIgnoreCase(searchRarityCB.get(i).getText()) && !searchRarityCB.get(i).isSelected()) || (cardData.getRarity().equalsIgnoreCase("Basic Land") && !searchRarityCB.get(0).isSelected())){
                    continue CardDataLoop;
                }
            }

            for(int i=0; i<searchTypeCB.size(); i++){
                if(cardData.getTypes() != null && cardData.getTypes().length > 0){
                    List<String> types = Arrays.asList(cardData.getTypes());
                    if(types.contains(searchTypeCB.get(i).getText()) && !searchTypeCB.get(i).isSelected()) {
                        continue CardDataLoop;
                    }
                }
            }

            for(String setCode : setCodes) {
                if (cardData.getSetCode().equalsIgnoreCase(setCode)) {
                    cards.add(cardData);
                    break;
                }
            }
        }
    }

    private void updateCardListView(ListView listView, ArrayList<CardData> cards){
        listView.getItems().clear();

        cards.forEach(cardData->{
            BorderPane borderPane = new BorderPane();
            HBox manaHBox = new HBox();
            Label cardLabel = new Label();
            Label setCodeLabel = new Label();
            setCodeLabel.setVisible(false);
            setCodeLabel.setText(Integer.toString(cards.indexOf(cardData)));

            if(cardData.getNames() != null && cardData.getNames().length > 1){
                String text = "";
                for(int i=0; i<cardData.getNames().length; i++){
                    text += cardData.getNames()[i];
                    if(i < cardData.getNames().length-1){
                        text += " // ";
                    }
                }
                cardLabel.setText(text);
            }else {
                cardLabel.setText(cardData.getName());
            }

            Group group = new Group();
            ImageView typeImageView = new ImageView();
            group.getChildren().add(typeImageView);
            cardLabel.setGraphic(group);

            if(cardData.getTypes() != null && cardData.getTypes().length > 0) {
                String mainType = cardData.getTypes()[0];
                if (symbols.containsKey(mainType)) {
                    typeImageView.setImage(symbols.get(mainType));
                }
            }

            if(rarityColor.containsKey(cardData.getRarity())){
                Rectangle rect = new Rectangle(0, 0, 15, 15);
                rect.setFill(rarityColor.get(cardData.getRarity()));
                rect.setBlendMode(BlendMode.ADD);
                group.getChildren().add(rect);
            }

            Pattern pattern = Pattern.compile("\\{(.*?)\\}");
            Matcher matcher = pattern.matcher(cardData.getManaCost());
            while(matcher.find()){
                String val = matcher.group(1).replaceAll("/", "");
                if(symbols.containsKey(val)){
                    manaHBox.getChildren().add(new ImageView(symbols.get(val)));
                }else{
                    manaHBox.getChildren().add(new ImageView(symbols.get("CHAOS")));
                }
            }

            borderPane.setLeft(cardLabel);
            borderPane.setCenter(setCodeLabel);
            borderPane.setRight(manaHBox);
            listView.getItems().add(borderPane);
        });

        listView.getSelectionModel().selectFirst();
        updateCardPreview(listView, cards);
    }

    private void updateDeckCardListView(ListView listView, HashMap<CardData, Integer> cards, int index){
        updateDeckCardListView(listView, cards);
        listView.getSelectionModel().select(index);
        updateCardPreview(listView, cards);
    }

    private void updateDeckCardListView(ListView listView, HashMap<CardData, Integer> cards){
        listView.getItems().clear();

        ArrayList<CardData> cardList = getSortedDeck(cards);

        cardList.forEach(cardData->{
            BorderPane borderPane = new BorderPane();
            HBox manaHBox = new HBox();
            Label cardCountLabel = new Label();
            cardCountLabel.setPadding(new Insets(0, 8, 0, 8));
            cardCountLabel.setMinWidth(48);
            cardCountLabel.setTextAlignment(TextAlignment.RIGHT);
            cardCountLabel.setText(Integer.toString(cards.get(cardData)));

            Label cardLabel = new Label();
            Label setCodeLabel = new Label();
            setCodeLabel.setVisible(false);
            setCodeLabel.setText(Integer.toString(cardList.indexOf(cardData)));

            if(cardData.getNames() != null && cardData.getNames().length > 1){
                String text = "";
                for(int i=0; i<cardData.getNames().length; i++){
                    text += cardData.getNames()[i];
                    if(i < cardData.getNames().length-1){
                        text += " // ";
                    }
                }
                cardLabel.setText(text);
            }else {
                cardLabel.setText(cardData.getName());
            }

            Group group = new Group();
            ImageView typeImageView = new ImageView();
            group.getChildren().add(typeImageView);
            cardLabel.setGraphic(group);

            if(cardData.getTypes() != null && cardData.getTypes().length > 0) {
                String mainType = cardData.getTypes()[0];
                if (symbols.containsKey(mainType)) {
                    typeImageView.setImage(symbols.get(mainType));
                }
            }

            if(rarityColor.containsKey(cardData.getRarity())){
                Rectangle rect = new Rectangle(0, 0, 15, 15);
                rect.setFill(rarityColor.get(cardData.getRarity()));
                rect.setBlendMode(BlendMode.ADD);
                group.getChildren().add(rect);
            }

            Pattern pattern = Pattern.compile("\\{(.*?)\\}");
            Matcher matcher = pattern.matcher(cardData.getManaCost());
            while(matcher.find()){
                String val = matcher.group(1).replaceAll("/", "");
                if(symbols.containsKey(val)){
                    manaHBox.getChildren().add(new ImageView(symbols.get(val)));
                }else{
                    manaHBox.getChildren().add(new ImageView(symbols.get("CHAOS")));
                }
            }

            BorderPane borderPane2 = new BorderPane();
            borderPane2.setLeft(cardCountLabel);
            borderPane2.setCenter(cardLabel);

            borderPane.setLeft(borderPane2);
            borderPane.setCenter(setCodeLabel);
            borderPane.setRight(manaHBox);
            listView.getItems().add(borderPane);
        });

        updateCardPreview(listView, cards);
    }

    private void loadAllImages(HashMap<String, Image> symbols, String dir) throws IOException {
        String path = "symbols";
        File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        if(jarFile.isFile()) {  // Run with JAR file
            final JarFile jar = new JarFile(jarFile);
            final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            while(entries.hasMoreElements()) {
                final String name = entries.nextElement().getName();
                if (name.startsWith(path + "/")) { //filter according to the path
                    if (name.contains(".")) {
                        String clipped = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
                        symbols.put(clipped, new Image(name));
                    }
                }
            }
            jar.close();
        } else { // Run with IDE
            final URL url = getClass().getResource("/" + path);
            if (url != null) {
                try {
                    final File apps = new File(url.toURI());
                    for (File app : apps.listFiles()) {
                        String name = app.toString();
                        String clipped = name.substring(name.lastIndexOf("\\") + 1, name.lastIndexOf("."));
                        symbols.put(clipped, new Image(new File(name).toURI().toString()));
                    }
                } catch (URISyntaxException ex) {
                    // never happens
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
