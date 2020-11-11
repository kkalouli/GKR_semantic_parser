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
   <!-- Example code -->
   <script type="text/javascript">
    	
   </script>

</head>
 
	<body style="background-color:#ffe4b2"  >  <!--the following is needed if the graphs should be loaded at once: onload="loadAllXmls()"  -->

 	<h1 class="tab" id="top"> <font color="#349aff">
		<big>G</big><small>raphical</small> <big>K</big><small>nowledge</small>
		<big>R</big><small>epresentations</small> <small>for</small>  <big>N</big><small>atural</small>
		<big>L</big><small>anguage</small> <big>I</big><small>nference</small> </font>
	</h1>

	<h3 class="tab" id="File"><font color="#349aff"> <a href="processed.txt" download>Download </a> <small> the file you just processed. </small> </font> </h3>
	<h3 class="tab" align="left"> <small> Do the GKR graphs seem wrong to you? Let us know <a
			href="mailto:aikaterini-lida.kalouli@uni-konstanz.de"> why!</a> </small> </h3>
	<form class="tab" action="index.jsp">
	 <button class="btn btn-primary" type="submit">Home</button>
</form>	

	<% if(request.getAttribute("error") != null){ %>
			<h2 class="tab" style="color:red">${error}</h2>
		<% } %>	
		

    </body>
</html>