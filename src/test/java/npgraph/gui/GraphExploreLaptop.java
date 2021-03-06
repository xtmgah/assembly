package npgraph.gui;

import java.io.IOException;

import org.graphstream.ui.view.Viewer;
import org.rtassembly.npgraph.BDGraph;
import org.rtassembly.npgraph.GraphUtil;
import org.rtassembly.npgraph.HybridAssembler;


public class GraphExploreLaptop {


	public static void main(String args[]) {
    	try {
			new GraphExploreLaptop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    }

    public GraphExploreLaptop() throws IOException{
    	System.setProperty("org.graphstream.ui", "javafx");
    	//System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer"); 
    	
    	/***********************************************************************
    	 * *********************    npscarf    *********************************
    	 ***********************************************************************/
    	String dataFolder="/home/s_hoangnguyen/Projects/scaffolding/test-graph/spades/"; //dell FASTG
//    	String dataFolder="/home/s_hoangnguyen/Projects/scaffolding/test-graph/spades_v3.10/"; //dell GFA

    	String sample="EcK12S-careful";
//    	String sample="Kp2146-careful";
//    	String sample="Kp13883-careful";
//
//    	String sample="W303-careful";
//    	String sample="meta-careful";
//    	String sample="cp_S5";
    	
		String 	sInput=dataFolder+sample+"/assembly_graph.fastg",
//    			lInput=dataFolder+sample+"/assembly_graph.sam";
				lInput="/home/s_hoangnguyen/Projects/scaffolding/test-graph/reads/EcK12S_ONT.fastq";
    	
    	/***********************************************************************
    	 * *********************    unicycler    *******************************
    	 ***********************************************************************/
//    	String dataFolder="/home/s_hoangnguyen/Projects/scaffolding/test-graph/unicycler/"; //unicycler synthetic
//    	String sample="E_coli_O25b_H4-ST131/good";
////    	String sample="Shigella_dysenteriae_Sd197/good";
////    	String sample="Shigella_sonnei_53G/good";
//    	String 	sInput=dataFolder+sample+"/spades/assembly_graph.fastg",
//    			lInput=dataFolder+sample+"/mm2.sam";
		
		
		/**********************************************************************************
		 * Share code for test all kind of data set
		 **********************************************************************************/
		HybridAssembler hbAss = new HybridAssembler();
		hbAss.setShortReadsInput(sInput);
		hbAss.setPrefix(dataFolder+sample+"/");
		hbAss.setShortReadsInputFormat("fastg");
		hbAss.setAligner("bwa");
		hbAss.setAlignerPath("/home/s_hoangnguyen/workspace/bwa/");
		hbAss.prepareShortReadsProcess();//change true/false to use/not use SPAdes path
		
		
    	BDGraph graph= hbAss.simGraph;
   
    	graph.setAttribute("ui.style", GraphUtil.styleSheet);
        Viewer viewer=graph.display();
        System.out.println("Node: " + graph.getNodeCount() + " Edge: " + graph.getEdgeCount());
                
        /*
         * Testing reduce function
         */
        try {
        	hbAss.setLongReadsInput(lInput);
//        	hbAss.setLongReadsInputFormat("sam");
        	hbAss.prepareLongReadsProcess();
        	
        	hbAss.assembly();
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

        System.out.println("Node: " + graph.getNodeCount() + " Edge: " + graph.getEdgeCount());
        
//        HybridAssembler.promptEnterKey();
        hbAss.postProcessGraph();
        
//    	GraphUtil.redrawGraphComponents(graph);
//    	graph.setAttribute("ui.style", GraphUtil.styleSheet);
//        Viewer viewer=graph.display();
        
        HybridAssembler.promptEnterKey();
        viewer.disableAutoLayout();
    }
    

}