# semantic_processing

How to build the project: (assumes you have java and gradle installed)
- Clone the repository.
- Go into the root folder (semantic_processing) of the cloned repository and do:
gradle build
- If the build is successful, you can simply import the project semantic_project into Eclipse. You should import it as a gradle project.
(for changes made within eclipse, the 'gradle cleanEclipse' command should refresh the eclipse view)

How to test a sentence:
- In the subproject sem.mapper there is a class named DepGraphToSemanticGraph.
- There is a main method at the end of this class where you can a)type the sentence you want to process and b)comment in or out the 
graphs that you would like to have displayed

Hopefully, gradle and my setup won't disappoint us :)

