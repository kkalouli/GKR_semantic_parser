# About

This is the Graphical Knowledge Representation (GKR) parser. It transforms a given sentence into a layered semantic graph as described
in *Kalouli, A.-L. and R. Crouch. 2018. GKR: the Graphical Knowledge Representation for semantic parsing. In Proceedings of SEMBEaR 2018
@NAACL 2018* : http://aclweb.org/anthology/W18-1304 

Companion paper: *Crouch, R. and A.-L. Kalouli. 2018. Named Graphs for Semantic Representations. In Proceedings of \*SEM 2018
@NAACL 2018* : http://aclweb.org/anthology/S18-2013 

Application paper: *Kalouli, A.-L., R. Crouch and V. de Paiva. GKR: Bridging the gap between symbolic/structural and distributional meaning representations. In
Proceedings of the 1st International Workshop on Designing Meaning Representations @ ACL 2019* : https://www.aclweb.org/anthology/W19-3305/

Author/developer: Aikaterini-Lida Kalouli (<aikaterini-lida.kalouli@uni-konstanz.de>) and Richard Crouch (<dick.crouch@gmail.com>)

If you use this software in writing scientific papers, or you use this
software in any other medium serving scientists or students (e.g. web-sites,
CD-ROMs) please include the above citation.

# Demo
If you would like to have a quick sense of how GKR looks like, check out our online demo: http://lap0973.sprachwiss.uni-konstanz.de:8080/sem.mapper/

# License
Copyright 2018 Aikaterini-Lida Kalouli and Richard Crouch. GKR is a free-software discributed under the conditions of the Apache License 2.0, without warranty. See LICENSE file for more details. You should have received a copy of the LICENSE file with the source code. If not, please visit http://www.apache.org/licenses/ for more information. 

# Installation 

(this installation assumes you have a Java runtime environment (version 8 or higher, <https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html> ) and the Gradle Build tool (<https://gradle.org/>) in place. If not, please make sure to install those first)

1. Clone the repository into your desired directory: ``` git clone https://github.com/kkalouli/GKR_semantic_parser.git ```

2.  Download WordNet from https://wordnet.princeton.edu/download.
In order to use WordNet, you must configure the file *GKR_semantic_parser/sem.mapper/src/main/resources/gkr.properties*. 
In particular you must change the following line:

``` wn_location=/usr/local/Cellar/wordnet/3.1/dict ```

Change the location and set the directory in which the WordNet dictionary is installed.

3. Download and install JIGSAW as described in <https://github.com/pippokill/JIGSAW> 
Although you will have downloaded WordNet by now, make sure you follow the JIGSAW instructions for modifying the JIGSAW config file in
*JIGSAW/resources/wn_file_properties.xml* . Also, in the *jigsaw.properties* file replace the relative paths of *wn_file_properties.xml*, *nlp.tokenModel*, *nlp.posTagModel* and 
*nlp.stopWordFile* with the absolute paths of their location in your system. In the same file, make sure that the property *wsd.shortOutput* is set to false (*wsd.shortOutput=false*).

In order to use JIGSAW, you must also configure the file *GKR_semantic_parser/sem.mapper/src/main/resources/gkr.properties*. 
In particular you must change the following line:

``` jigsaw_props=/Users/kkalouli/Documents/libraries/JIGSAW-master/resources/jigsaw.properties ```

Change the properties file name and set the absolute path in which your *jigsaw.properties* file is installed. 

4. Download and install SUMO as described in https://github.com/ontologyportal/sigmakee (you don't need to install the apache-tomcat related stuff if we you don't want to run the SUMO interface through your localhost). Download the file *WordNetMappings30-all.txt* provided here and put it in SUMO's folder *WordNetMappings* (this is a combined version of the four separate files *WordNetMappings30-noun/verb/adj/adv* that SUMO itself provides and is used as a consolidated verison when a term cannot be found in the file of its corresponding POS.)  
In order to use SUMO, you must also configure the file *GKR_semantic_parser/sem.mapper/src/main/resources/gkr.properties*. 
In particular you must change the following line:

``` sumo_location=/Users/kkalouli/Documents/workspace/sumo ```

Change the location and set the location path in which you installed the parent SUMO folder. 

5. Download the jar file *edu.mit.jwi_2.4.0.jar* from <https://projects.csail.mit.edu/jwi/>

6. Create a new folder *GKR_libs* somewhere in your system. Copy in this folder the following files:
- the *edu.mit.jwi_2.4.0.jar* jar file you just downloaded
- the *JIGSAW.jar* file (found in the folder *dist* of your JIGSAW installation)
- the *maxent-3.0.0.jar* file (found in the folder *dist/lib/* of your JIGSAW installation)
- *opennlp-tools-1.5.0.jar* (found in the folder *dist/lib/* of your JIGSAW installation)

7. The GKR parser uses the easy-bert software by Rob Rua (https://zenodo.org/record/2652964#.X6pxb5NKjR0). The software is imported through gradle and the bert vocabulary file for the BERT uncases model is already provided in *sem.mapper/src/main/resources/vocab.txt* . If you wish to use a different model,  you will need to download the desired model from https://github.com/robrua/easy-bert and unzip it. Once you unzip it, you will see a file *vocab.txt* within the folder. You will need to configure the file *GKR_semantic_parser/sem.mapper/src/main/resources/gkr.properties* and in particular, change the following line:

``` bert_vocab=/Users/kkalouli/Documents/project/sem.mapper/src/main/resources/vocab.txt ```  

to point to the location of the *vocab.txt* file. You will also need to modify the *build.gradle* file of sem.mapper to import the model you want (*implementation 'com.robrua.nlp.models:easy-bert-uncased-L-12-H-768-A-12:1.0.0'* )

************** Deprecated ******************
Download the *glove.6B.zip* from <https://nlp.stanford.edu/projects/glove/> and unzip it. Choose the size of the embedding file you want to work with and copy this file into *GKR_semantic_parser/sem.mapper/src/main/resources/*. The default file chosen is the *glove.6B.300d.txt*. If you choose this file, you don't need to do anything further. If * you choose another size file, you have to also configure the file *GKR_semantic_parser/sem.mapper/src/main/resources/gkr.properties*. In particular you must change the line * ``` glove=../sem.mapper/src/main/resources/glove.6B.300d.txt``` to the name of the file you chose. 



8. Go back into the cloned directory and find the *build.gradle* file of the *sem.mapper* folder. Change the following line
```compile fileTree(dir: '/Users/kkalouli/Documents/libraries/GKR_libs/', include: ['*.jar']) ```
to point at the location in which you created the *GKR_libs* folder. 

You are all set! You can now build the project:
Navigate to the cloned directory (*GKR_semantic_parser*) and do: ``` gradle build ```

If the build is successful, you can simply import the project in Eclipse or any other platform you wish.
For Eclipse: import it as a gradle project
(for changes made within eclipse, the 'gradle cleanEclipse' command should refresh the eclipse view)



# Usage

In the subproject sem.mapper there is a class named DepGraphToSemanticGraph.
In the main method at the end of this class you can choose to run only one sentence or a testsuite with sentences: just comment in or
out the corresponding method. The testsuite should contain one sentence per line.

# Evaluation
The testsuite used in our paper *Kalouli, A.-L. and Richard Crouch. 2018. GKR: the Graphical Knowledge Representation for semantic parsing. In Proceedings of SEMBEaR 2018
@ NAACL 2018* (http://aclweb.org/anthology/W18-1304) can be found in the folder *evaluation*. 

# Contact
For troubleshooting, comments, ideas and discussions, please contact aikaterini-lida.kalouli(at)uni-konstanz.de or dick.crouch(at)gmail.com







