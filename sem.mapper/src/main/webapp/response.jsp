<%@ page contentType="text/html; charset=iso-8859-1" language="java" %>
<html>
<head>
<title>GKR Parser</title>
<meta charset="UTF-8"/>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
<style type="text/css">

.tab { margin-left: 30px; }

.block { margin-left: 30px; position: fixed; }

#box { position: relative; margin-left: 30px; }

</style>

<!-- Sets the basepath for the library if not in same directory -->
   <script type="text/javascript">
      mxBasePath = 'src';
   </script>

   <!-- Loads and initializes the library -->
   <script type="text/javascript" src="mxClient.js"></script>

   <!-- Example code -->
   <script type="text/javascript">
    	
   // loads the created xml and decodes it into a mxgrah
        function loadXML(container,xmlString){
	   // check if browser is supported
    	  if (!mxClient.isBrowserSupported())
          {
             mxUtils.error('Browser is not supported!', 200, false);
             return 0;
          }
          else
          {
        	  // create new graph
	    	  var graph = new mxGraph(container);
	          new mxRubberband(graph);
	          // parse xml to a Document
	    	  var doc = mxUtils.parseXml(xmlString);
	    	  var codec = new mxCodec(doc);
	    	  // get first elements of Documents
	    	  var parent = graph.getDefaultParent();
	    	  var firstChild = doc.documentElement.firstChild;
	    	  var root = doc.documentElement.childNodes[1];
	    	  var id0 = root.childNodes[1];
	    	  var id1 = id0.childNodes[1];
	    	  //console.log(doc);
	    	  //console.log(parent);
	    	  //console.log(firstChild);
	    	  //console.log(root);
	    	  //console.log(id0);
	    	  //console.log(id1);
	    	  //console.log(graph);
	    	  // dict of node names as keys and vertics as values
	    	  var dictNodes = {};
	    	  // all the edges that need to be added
	    	  var edges = [];
	    	  // the nodes that have already been added
	    	  var addedNodes = [];
	 
	    	  // go through each of the children and get the attributes
	    	  for (var i = 1; i < id1.childNodes.length; i+=2) {
	    	      console.log("Node ID: " + i);
	    	      var cell = codec.decode(id1.childNodes[i]);
	    	      if (cell != null){
	    	    	 // check if this cell is an edge or a vertex
	    	    	var isEdge = cell.hasAttribute("edge");
	    	    	graph.getModel().beginUpdate();
	    	    	// only add vertex if it doesnt exist
	    	    	if (isEdge == false && addedNodes.includes(cell) == false ){
	    	    		console.log(cell);
	    	    		var value = graph.insertVertex(parent, null, cell.getAttribute("value"), 200, 200, 80, 30, cell.getAttribute("style"), cell.getAttribute("relative"));
	    	    		graph.updateCellSize(value, true);
	    	    		var geom = value.getGeometry();
	    	    		geom.width = geom.width > 80 ? geom.width : 80;
	    	    		geom.height = geom.height > 30 ? geom.height : 30;
	    	    		dictNodes[cell.getAttribute("id")] = value;
	    	    		addedNodes.push(cell);
	    	    	} else {
	    	    		// if it is an edge, push to the edges
	    	    		edges.push(cell);
	    	    	}
	    	    	graph.getModel().endUpdate();
	       	      }
	    	  }
	    	  console.log(edges);
	    	  //console.log(dict);
	    	  // go thrugh the edges and add them, get the source and the tagrt from the dict
	    	  for (var i = 0; i < edges.length; i++) {
	    		  var source = edges[i].getAttribute("source");
	    		  var target = edges[i].getAttribute("target");
	    		  var sourceNode = dictNodes[source];
	    		  var targetNode = dictNodes[target];
	    		  graph.insertEdge(parent, null, edges[i].getAttribute("value"), sourceNode, targetNode);
	    	  }
	    	  // hierarchical layout
			  var layout = new mxHierarchicalLayout(graph);
			  layout.execute(parent);
	    	  graph.maximumGraphBounds = new mxRectangle(graph.getGraphBounds().getX, graph.getGraphBounds().getY, graph.getGraphBounds().width, graph.getGraphBounds().height);
	    	  //graph.getView().setGraphBounds(test);
	    	  var bounds =  graph.getMaximumGraphBounds();
	    	  console.log(bounds);
  	  		  return graph.getMaximumGraphBounds().height+60;
          }
			
      }
   
      
   // load all xmls of all graphs 
      function loadAllXmls(){
    	  loadXML(document.getElementById('plainContainer'), document.getElementById('depsGraph').innerHTML);
    	  loadXML(document.getElementById('plainContainer'), document.getElementById('roleGraph').innerHTML);
    	  loadXML(document.getElementById('plainContainer'), document.getElementById('ctxGraph').innerHTML);
    	  loadXML(document.getElementById('plainContainer'), document.getElementById('propsGraph').innerHTML);
    	  loadXML(document.getElementById('plainContainer'), document.getElementById('lexGraph').innerHTML);
    	  loadXML(document.getElementById('plainContainer'), document.getElementById('corefGraph').innerHTML);
      }
   </script>

</head>
 
	<body style="background-color:#ffe4b2"  >  <!--the following is needed if the graphs should be loaded at once: onload="loadAllXmls()"  -->

 	<h1 class="tab" id="top"> <font color="#349aff">
		<big>G</big><small>raphical</small> <big>K</big><small>nowledge</small>
		<big>R</big><small>epresentations</small> <small>for</small>  <big>N</big><small>atural</small>
		<big>L</big><small>anguage</small> <big>I</big><small>nference</small> </font>
	</h1>

	<h3 class="tab" id="sentence"><font color="#349aff">Sentence: </font> </h3><p class="tab">${sent} </p> <br>
	<form class="tab" action="index.jsp">
	 <button class="btn btn-primary" type="submit">Home</button>
</form>

	<h3 class="tab" align="left"> <small> Does this GKR graph seem wrong to you? Let us know <a
			href="mailto:aikaterini-lida.kalouli@uni-konstanz.de"> why!</a> </small> </h3>
			
	<h3 class="tab" align="left"> <small> Feel free to move the nodes and the edges to get a better view, if needed. </small> </h3>

	<% if(request.getAttribute("error") != null){ %>
			<h2 class="tab" style="color:red">${error}</h2>
		<% } %>	
	
	<div id="depsGraph">${depsGraph}</div>
	<div id="roleGraph">${roleGraph}</div>
	<div id="ctxGraph">${ctxGraph}</div>
	<div id="propsGraph">${propsGraph}</div>
	<div id="lexGraph">${lexGraph}</div>
	<div id="corefGraph">${corefGraph}</div>
	<div id="rolesAndCtxGraph">${rolesAndCtxGraph}</div>
	<div id="rolesAndCorefGraph">${rolesAndCorefGraph}</div>
	
	<div id="plainContainer" style="overflow:auto;width:100%;height:70%;padding-left:80px;">   
	   <!-- Creates a container for the graph with a grid wallpaper style="overflow:auto;width:1000;height:200px"  -->
   	 
   	<div id="depsContainer" style="width:auto;"> 
   		<h2> Dependency Graph  </h2>
		<script type="text/javascript">
		var height = loadXML(document.getElementById('depsContainer'), document.getElementById('depsGraph').innerHTML); 
		document.getElementById('depsContainer').style.height = height + "px";
		</script>
	</div>
	
	<div id="roleContainer" style="width:auto;"> 
		<h2> Concept Graph</h2>
		<script type="text/javascript"> 
		var height = loadXML(document.getElementById('roleContainer'), document.getElementById('roleGraph').innerHTML);
		document.getElementById('roleContainer').style.height = height + "px";
		</script>
	</div>
	
	<div id="ctxContainer" style="width:auto;"> 
		<h2>Context Graph</h2>
		<script type="text/javascript"> 
		var height = loadXML(document.getElementById('ctxContainer'), document.getElementById('ctxGraph').innerHTML); 
		document.getElementById('ctxContainer').style.height = height + "px";
		</script>
	</div>
	
	<div id="rolesAndCtxContainer" style="width:auto;"> 
		<h2>Concept and Context Graph (merged)</h2>
		<script type="text/javascript"> 
		var height = loadXML(document.getElementById('rolesAndCtxContainer'), document.getElementById('rolesAndCtxGraph').innerHTML); 
		document.getElementById('rolesAndCtxContainer').style.height = height + "px";
		</script>
	</div>
	
	<div id="propsContainer" style="width:auto;"> 
		<h2 >Properties Graph</h2>
		<script type="text/javascript"> 
		var height = loadXML(document.getElementById('propsContainer'), document.getElementById('propsGraph').innerHTML); 
		document.getElementById('propsContainer').style.height = height + "px";
		</script>
	</div>
	
	<div id="lexContainer" style="width:auto;"> 
		<h2 >Lexical Graph</h2>
		<script type="text/javascript"> 
		var height = loadXML(document.getElementById('lexContainer'), document.getElementById('lexGraph').innerHTML);
		document.getElementById('lexContainer').style.height = height + "px";
		</script>
	</div>
	
	<div id="corefContainer" style="width:auto;">
		<h2>Coreference Graph</h2>
		<script type="text/javascript"> 
		var height = loadXML(document.getElementById('corefContainer'), document.getElementById('corefGraph').innerHTML);
		document.getElementById('corefContainer').style.height = height + "px";
		</script>
	</div>
	
	<div id="rolesAndCorefContainer" style="width:auto;">
		<h2>Concept and Coreference Graph (merged)</h2>
		<script type="text/javascript"> 
		var height = loadXML(document.getElementById('rolesAndCorefContainer'), document.getElementById('rolesAndCorefGraph').innerHTML);
		document.getElementById('rolesAndCorefContainer').style.height = height + "px";
		</script>
	</div>
	
   </div>
    

	 <!-- the following is needed if we want to have separate containers for each graph 
	<div id="depsContainer" style="width:auto;height:auto;padding-left:80px;padding-bottom:50px"> </div>
   	<div id="roleContainer"> </div>
	<div id="ctxContainer" style="width:auto;height:auto;padding-left:80px;padding-bottom:50px"> </div>
	<div id="propsContainer" style="width:auto;height:auto;padding-left:80px;padding-bottom:50px"> </div>
	<div id="lexContainer" style="width:auto;height:auto;padding-left:80px;padding-bottom:50px"> </div>
	<div id="corefContainer" style="width:auto;height:auto;padding-left:80px;padding-bottom:50px"> </div>
   -->
	

    </body>
</html>