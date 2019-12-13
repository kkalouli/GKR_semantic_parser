<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
<style type="text/css">
#loader {
display: none;
display: none;
position: fixed;
top: 0;
left: 0;
right: 0;
bottom: 0;
width: 100%;
background: rgba(0,0,0,0.75) url(images_default/bunny_loop.gif) no-repeat center center;
z-index: 10000;
}

 .tab { margin-left: 30px; }
 
</style>
</head>
<body style="background-color:#ffe4b2">

   
<div id="loader"></div>
	<h1 class="tab" id="top" >
		<font color="#349aff"><big>G</big><small>raphical</small> <big>K</big><small>nowledge</small>
		<big>R</big><small>epresentations</small> <small>for</small>  <big>N</big><small>atural</small>
		<big>L</big><small>anguage</small> <big>I</big><small>nference</small> </font>
	</h1>

	<p class="tab">
		This is the Graphical Knowledge Representation (GKR) parser. It
		transforms a given sentence into a layered semantic graph. The
		semantic graph (currently) consists of 6 subgraphs/layers: dependency graph,
		concept graph, context graph, lexical graph, properties graph and
		coreference graph. This separation in layers is analogous to the separation in levels
		in the LFG architecture (Kaplan, 1995): each layer encodes different kinds of
		information present in the sentence and this allows the formulation of modular linguistic
		generalizations which govern a given layer independently from the others.
		This allows for the combination of multiple logics and styles of representations, i.e.
		structural/linguistic and distributional representations, and contrasts with the latent representations used
		in end-to-end deep learning approaches. The
		representation is especially targeted towards Natural Language
		Inference (NLI) but is also suitable for other semantic processing tasks, e.g. semantic similarity tasks. <br>
		The current version of GKR makes uses of the Stanford CoreNLP 3.8.0 software to build the dependency graph. It also uses the Princeton WordNet 3.0
		version to extract the senses of the lexical graph. For more details on how this software is used, please refer to our publications. 
		This demo seeks to give interested researchers a taste of GKR. Currently, GKR can deal with various contextual phenomena, such as negation, 
		modals, clausal contexts of propositional attitudes (e.g. belief, knowledge, obligation),  implicatives, interrogative and imperative clauses, 
		disjunction and conjunction. The treatment of conditionals, distributivity and ellipsis is not implemented yet, but planned for the future.     
		GKR is constantly being improved and we are thankful for any comments or discussions. <br> We are currently
		also implementing a hybrid, symbolic and distributional, NLI system
		based on GKR. Its preliminary version will be made available soon. 
	</p>

	</p>
	
	<h2 class="tab" id="download"><font color="#349aff">Download</font></h2>
	<p class="tab">
		The source code of GKR is publicly available on <a
			href="https://github.com/kkalouli/GKR_semantic_parser"> github</a>.
	</p>

	<br>

	<h2 class="tab" id="online"><font color="#349aff">Online Demo for GKR</font></h2>
	<p class="tab">
		Enter a sentence below to try our GKR parser online:<br>
		<form class="tab" method="post" action="gkr">  
  <input type="text" id="sentence-input" name="sentence" />
   <!-- <input type="submit" id="process-button" value="Process" /> -->
     <button class="btn btn-primary mb1 bg-blue" type="submit">Submit</button>
</form>

<br>


	<h3 class="tab"><font color="#349aff">Examples</font></h3>
	<form class="tab" method="post" action="gkr"> 
  <input type="radio" name="id" value="-1" checked> The boy faked the illness. <br>
  <input type="radio" name="id" value="-2"> Negotiations prevented the strike.<br>
  <input type="radio" name="id" value="-3"> The dog is not eating the food. <br>
  <input type="radio" name="id" value="-4"> John or Mary won the competition.<br>
  <input type="radio" name="id" value="-5"> No woman is walking.<br>
  <input type="radio" name="id" value="-6"> Max forgot to close the door.<br>
  <input type="radio" name="id" value="-7"> John might apply for the position.<br>
  <input type="radio" name="id" value="-8"> Did Ann manage to close the window?<br>
  <input type="radio" name="id" value="-9"> Fred believes that John doesn't love Mary.<br>
  <input type="radio" name="id" value="-10"> The boy and the girl are walking. <br>
  <input type="radio" name="id" value="-11"> Nicole Kidman, the actress, won the oscar.<br>
  <input type="radio" name="id" value="-12"> Be patient! <br>
   <input type="radio" name="id" value="-13"> The director, who edited the first movie, released the second part. <br>
  <button class="btn btn-primary" type="submit">Submit</button>
	</form>
	</p>
	
	<br>

	<h2 class="tab" id="publications"><font color="#349aff">Publications</font></h2>
	<ul>
		<li> GKR: Bridging the gap between symbolic/structural and distributional meaning representations</a> Kalouli,
			A.-L., Richard Crouch and Valeria de Paiva. 2019, to appear. 1st International Workshop on Designing Meaning Representations (DMR) @ACL 2019.
		<li><a href="http://aclweb.org/anthology/W18-1304"> GKR: the
				Graphical Knowledge Representation for semantic parsing</a> Kalouli,
			A.-L. and Richard Crouch. 2018. SEMBEaR @NAACL 2018.
		<li><a href="http://aclweb.org/anthology/S18-2013"> Named
				Graphs for Semantic Representations </a> Crouch, R. and A.-L. Kalouli.
			2018. *SEM 2018.
		<li><a href="https://easychair.org/publications/preprint/rXqs">
				Graph Knowledge Representations for SICK </a> Kalouli, A.-L., Richard
			Crouch, Valeria de Paiva and Livy Real. 2018. 5th Workshop on Natural
			Language and Computer Science @FLoC 2018
	</ul>


	<br>

	<h2 class="tab" id="contact"><font color="#349aff">Contact</font></h2>
	<ul>
		<li>aikaterini (dash) lida (dot) kalouli (at) uni (dash) konstanz
			(dot) de
		<li>dick (dot) crouch (at) gmail (dot)com
	</ul>
	
		<br>
	<br>
	
	<p align="center"> Copyright 2018 Aikaterini-Lida Kalouli and Richard Crouch </p>
	

<script src="http://code.jquery.com/jquery.js"></script>
 
<!-- Latest compiled and minified JavaScript  -->
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
<script>
var spinner = $('#loader');
$(function() {
  $('form').submit(function(e) {
    spinner.show();
  });
});
</script>


</body>
</html>