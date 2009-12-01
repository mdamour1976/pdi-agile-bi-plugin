package org.pentaho.agilebi.pdi.modeler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.agilebi.pdi.visualizations.IVisualization;
import org.pentaho.agilebi.pdi.visualizations.VisualizationManager;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.binding.DefaultBindingFactory;
import org.pentaho.ui.xul.binding.Binding.Type;
import org.pentaho.ui.xul.components.XulLabel;
import org.pentaho.ui.xul.components.XulMenuList;
import org.pentaho.ui.xul.components.XulTextbox;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.containers.XulListbox;
import org.pentaho.ui.xul.containers.XulTabbox;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.impl.AbstractXulEventHandler;

/**
 * XUL Event Handler for the modeling interface. This class interacts with a ModelerModel to store state.
 * 
 * @author nbaker
 *
 */
public class ModelerController extends AbstractXulEventHandler{

  private static final String NEW_DIMENSION_NAME = "newDimensionName"; //$NON-NLS-1$

  private static final String NEW_DIMESION_DIALOG = "newDimesionDialog"; //$NON-NLS-1$


  private static final String MY_TAB_LIST_ID = "myTabList"; //$NON-NLS-1$

  private static final String FIELD_LIST_ID = "fieldList"; //$NON-NLS-1$

  private static final String IN_PLAY_TABLE_ID = "fieldTable"; //$NON-NLS-1$

  private static final String MODEL_NAME_FIELD_ID = "modelname"; //$NON-NLS-1$
  
  private static final String SOURCE_NAME_LABEL_ID = "source_name"; //$NON-NLS-1$

  private static final String MODEL_NAME_PROPERTY = "modelName"; //$NON-NLS-1$

  private static final String VALUE_PROPERTY = "value"; //$NON-NLS-1$

  private static final String ELEMENTS_PROPERTY = "elements"; //$NON-NLS-1$

  private static final String FIELD_NAMES_PROPERTY = "fieldNames"; //$NON-NLS-1$

  private static Log logger = LogFactory.getLog(ModelerController.class);
  
  private ModelerWorkspace model;
  
  private String fieldTypesDesc[];
  private Integer fieldTypesCode[];
  
  private XulDialog newDimensionDialog;
  private XulTextbox newDimensionName;
  private XulTree dimensionTree;
  private XulLabel detailsLabel;
  private XulLabel detailsLabel2;
  private XulLabel detailsLabel3;
  private XulMenuList serverList;
  private XulMenuList visualizationList;
  
  private BindingFactory bf = new DefaultBindingFactory();
 
  private List<String> serverNames;
  private List<String> visualizationNames;
  private List<String> serverIds;
  
  public ModelerController(){
    model = new ModelerWorkspace();
  }
  
  public ModelerController(ModelerWorkspace model){
    this.model = model;
  }
  
  public String getName(){
    return "modeler";
  }
  
  public void init() throws ModelerException{

    bf.setDocument(document);
    
    newDimensionDialog = (XulDialog) document.getElementById(NEW_DIMESION_DIALOG);
    newDimensionName = (XulTextbox) document.getElementById(NEW_DIMENSION_NAME);
    dimensionTree = (XulTree) document.getElementById("dimensionTree");
    detailsLabel = (XulLabel)document.getElementById("details");
    detailsLabel2 = (XulLabel)document.getElementById("details2");
    detailsLabel3 = (XulLabel)document.getElementById("details3");
    serverList = (XulMenuList)document.getElementById("serverlist");
    visualizationList = (XulMenuList)document.getElementById("visualizationlist");

    XulLabel sourceLabel = (XulLabel) document.getElementById(SOURCE_NAME_LABEL_ID);
    
    bf.createBinding(model, "sourceName", sourceLabel, "value");

    bf.setBindingType(Type.ONE_WAY);
    fieldListBinding = bf.createBinding(model, "availableFields", FIELD_LIST_ID, ELEMENTS_PROPERTY);
    
    // dimensionTable

    //bf.createBinding(model, "selectedServer", serverList, "selectedItem");    
    //serversBinding = bf.createBinding(this, "serverNames", serverList, "elements");
    
    bf.createBinding(model, "selectedVisualization", visualizationList, "selectedItem");    
    visualizationsBinding = bf.createBinding(this, "visualizationNames", visualizationList, "elements");
    
    dimensionTreeBinding = bf.createBinding(model, "dimensions", dimensionTree, "elements");
    bf.createBinding(dimensionTree, "selectedItem", this, "dimTreeSelectionChanged");
    
    bf.setBindingType(Type.BI_DIRECTIONAL);
    inPlayTableBinding = bf.createBinding(model, "fields" , "fieldTable", "elements");
    modelNameBinding = bf.createBinding(model, MODEL_NAME_PROPERTY, MODEL_NAME_FIELD_ID, VALUE_PROPERTY);
    
    fireBindings();
  }
  
  private void fireBindings() throws ModelerException{
    try {
      fieldListBinding.fireSourceChanged();
      dimensionTreeBinding.fireSourceChanged();
      inPlayTableBinding.fireSourceChanged();
      modelNameBinding.fireSourceChanged();
      //serversBinding.fireSourceChanged();
      visualizationsBinding.fireSourceChanged();
    } catch (Exception e) {
      logger.info("Error firing off initial bindings", e);
      throw new ModelerException(e);
    }
  }
  
  public void setSelectedDims(List<Object> selectedDims) {
    List<Object> prevSelected = null; // this.selectedColumns;
    if (selectedDims != null) {
      System.out.println(selectedDims.get(0));
    }
    // this.selectedColumns = selectedColumns;
    // this.firePropertyChange("selectedColumns", prevSelected , selectedColumns);
  }
  
  
  public void moveFieldIntoPlay() {
    XulListbox fieldsList = (XulListbox) document.getElementById(FIELD_LIST_ID);
    Object[] selectedItems = fieldsList.getSelectedItems();
    int tabIndex = ((XulTabbox) document.getElementById(MY_TAB_LIST_ID)).getSelectedIndex();
    if (tabIndex == 0) {
      for (Object obj : selectedItems) {
        model.addFieldIntoPlay(obj);
      }
    } else if (tabIndex == 1){
      // if a dimension or hierarchy is selected, add the field as a level
      // otherwise add a new dimension
      for (Object obj : selectedItems) {
        if (selectedTreeItem == null) {
          model.addDimension(obj);
        } else {
          model.addToHeirarchy(selectedTreeItem, obj);
        }
      }
    }
  }

  public void moveFieldOutOfPlay() {
    XulTree tree = (XulTree) document.getElementById(IN_PLAY_TABLE_ID);
    int[] selectedIndexes = tree.getSelectedRows();
    int tabIndex = ((XulTabbox) document.getElementById(MY_TAB_LIST_ID)).getSelectedIndex();
    if (tabIndex == 0) {
      for (int index : selectedIndexes) {
        FieldMetaData fieldMeta = model.getFields().get(index);
        model.removeFieldFromPlay(fieldMeta);
      }
    } else if (tabIndex == 1){
      if (selectedTreeItem instanceof LevelMetaData) {
        removeDimension();
      }
    }
  }
  
  public void visualize() throws ModelerException{
    try{
      openVisualizer();
    } catch(Exception e){
      logger.info(e);
      throw new ModelerException(e);
    }
  }
  
  public void publish() throws ModelerException{
    try{
      ModelerWorkspaceUtil.populateDomain(model);
    
      ModelServerPublish publisher = new ModelServerPublish();

      publisher.setBiServerId(model.getSelectedServer());
      publisher.publishToServer( model.getModelName() + ".mondrian.xml", model.getDatabaseName(), model.getModelName(), true );
    } catch(Exception e){
      logger.info(e);
      throw new ModelerException(e);
    }

  }
  
  /**
   * Goes back to the source of the metadata and see if anything has changed.
   * Updates the UI accordingly
   */
  public void refreshFields() throws ModelerException {

    model.refresh();
  }
  
  public void setFileName(String fileName){
    model.setFileName(fileName);
  }
  

  public void showNewDimensionDialog(){
    this.newDimensionDialog.show();
  }
  
  public void hideNewDimensionDialog(){
    this.newDimensionDialog.hide();
  }
  
  public void addNewDimension(){
    String dimName = this.newDimensionName.getValue();

    DimensionMetaData dimension = new DimensionMetaData(dimName);
    HierarchyMetaData hierarchy = new HierarchyMetaData(dimName);
    hierarchy.setParent(dimension);
    dimension.add(hierarchy);
    model.addDimension(dimension);
    
    hideNewDimensionDialog();
  }
  
  public void moveDimensionUp() {

    if (selectedTreeItem instanceof DimensionMetaData) {
      if(model.getDimensions().indexOf(selectedTreeItem) > 0){
        model.getDimensions().moveChildUp((DimensionMetaData) selectedTreeItem);
        setDimTreeSelectionChanged(null);
      }
    } else if (selectedTreeItem instanceof HierarchyMetaData) {
      DimensionMetaData dim = (DimensionMetaData) ((HierarchyMetaData)selectedTreeItem).getParent();
      if(dim.indexOf(selectedTreeItem) > 0){
        dim.moveChildUp((HierarchyMetaData)selectedTreeItem);
        setDimTreeSelectionChanged(null);
      }
    } else if (selectedTreeItem instanceof LevelMetaData) {
      HierarchyMetaData dim = (HierarchyMetaData) ((LevelMetaData)selectedTreeItem).getParent();
      if(dim.indexOf(selectedTreeItem) > 0){
        dim.moveChildUp((LevelMetaData)selectedTreeItem);
        setDimTreeSelectionChanged(null);
      }
    }
  }
  
  public void moveDimensionDown() {

    if (selectedTreeItem instanceof DimensionMetaData) {
      if(model.getDimensions().size() > model.getDimensions().indexOf(selectedTreeItem) + 1){
        model.getDimensions().moveChildDown((DimensionMetaData) selectedTreeItem);
        setDimTreeSelectionChanged(null);
      }
    } else if (selectedTreeItem instanceof HierarchyMetaData) {
      DimensionMetaData dim = (DimensionMetaData) ((HierarchyMetaData)selectedTreeItem).getParent();
      if(dim.size() > dim.indexOf(selectedTreeItem) +1){
        dim.moveChildDown((HierarchyMetaData)selectedTreeItem);
        setDimTreeSelectionChanged(null);
      }
    } else if (selectedTreeItem instanceof LevelMetaData) {
      HierarchyMetaData dim = (HierarchyMetaData) ((LevelMetaData)selectedTreeItem).getParent();
      if(dim.getChildren().size() > dim.getChildren().indexOf(selectedTreeItem) +1){
        dim.moveChildDown((LevelMetaData)selectedTreeItem);
        setDimTreeSelectionChanged(null);
      }
    }
  }

  Object selectedTreeItem;

  private Binding fieldListBinding;

  private Binding serversBinding;

  private Binding visualizationsBinding;

  private Binding dimensionTreeBinding;

  private Binding modelNameBinding;

  private Binding inPlayTableBinding;
  
  public void setDimTreeSelectionChanged(Object selection){
    selectedTreeItem = selection;
    detailsLabel.setValue("");
    detailsLabel2.setValue("");
    detailsLabel3.setValue("");
    if (selectedTreeItem != null) {
      String data[] = selectedTreeItem.toString().split("\n");
      detailsLabel.setValue(data[0]);
      if (data.length > 1) {
        detailsLabel2.setValue(data[1]);
        if (data.length > 2) {
          detailsLabel3.setValue(data[2]);
        }
      }
    }
  }
  

  public void removeDimension() {
    if (selectedTreeItem instanceof DimensionMetaData) {
      model.getDimensions().remove(selectedTreeItem);
    } else if (selectedTreeItem instanceof HierarchyMetaData) {
      ((HierarchyMetaData)selectedTreeItem).getParent().remove(selectedTreeItem);
    } else if (selectedTreeItem instanceof LevelMetaData) {
      ((LevelMetaData)selectedTreeItem).getParent().getChildren().remove(selectedTreeItem);
    }
    setDimTreeSelectionChanged(null);
  }

  public List<String> getServerNames() {
    return serverNames;
  }
  
  public List<String> getVisualizationNames() {
  	if(this.visualizationNames == null) {
  		VisualizationManager theManager = VisualizationManager.getInstance();
  		this.visualizationNames = theManager.getVisualizationNames();
  	}
  	return this.visualizationNames;
  }
  
  private void createServerList() {
    serverNames.clear();
    serverIds.clear();
    Spoon spoon = ((Spoon)SpoonFactory.getInstance());
    PropsUI props = spoon.getProperties();
    int idx = 1;
    boolean running = true;
    while( running ) {
      String serverId = "biserver"+Integer.toString(idx);
      String serverName = props.getProperty(serverId+"/name");
      if( serverName != null ) {
        serverNames.add( serverName );
        serverIds.add( serverId );
        idx++;
      } else {
        running = false;
      }
    }
  }

  public ModelerWorkspace getModel() {
    return model;
  }

  public void setModel(ModelerWorkspace model) throws ModelerException{
    this.model = model;
    fireBindings();
  }
  

  public void openVisualizer() {
  	VisualizationManager theManager = VisualizationManager.getInstance();
  	IVisualization theVisualization = theManager.getVisualization(visualizationList.getSelectedItem());
  	if(theVisualization != null) {
  	  if (model.getFileName() != null) {
  	    // TODO: Find a better name for the cube, maybe just model name?
  	    theVisualization.openVisualizer(model.getFileName(), model.getModelName() + " Cube");
  	  } else {
  	    throw new UnsupportedOperationException("TODO: prompt to save model before visualization");
  	  }
  	}
  }
  
  public void saveWorkspace(String fileName) throws ModelerException {
  	ModelerWorkspaceUtil.saveWorkspace(model, fileName);
    model.setFileName(fileName);
  }
  
  public void loadWorkspace() throws ModelerException {
  	
  	try {
	  	StringBuffer theStringBuffer = new StringBuffer();
	  	FileReader theReader = new FileReader(new File("my_metadata.xml"));
	  	BufferedReader theBuffer = new BufferedReader(theReader);
	  	String theLine = null;
	  	while((theLine = theBuffer.readLine()) != null) {
	  		theStringBuffer.append(theLine);
	  	}
	  	ModelerWorkspaceUtil.loadWorkspace("my_metadata.xml", theStringBuffer.toString(), getModel());
	  	
  	} catch(Exception e) {
  		logger.info(e.getLocalizedMessage());
  		new ModelerException(e);
  	}
  }
  
}
